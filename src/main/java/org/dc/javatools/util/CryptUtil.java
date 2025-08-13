package org.dc.javatools.util;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.ToString;
import lombok.experimental.Accessors;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.KeyGenerator;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.AlgorithmParameterSpec;
import java.util.Arrays;
import java.util.Random;

/**
 * {@link CryptUtil} 提供以 AES-GCM 演算法進行對稱式加解密的工具方法。
 * <p>
 * 目前採用固定的：
 * <ul>
 *   <li>加密演算法：AES</li>
 *   <li>工作模式：GCM (Galois/Counter Mode)</li>
 *   <li>填充方式：NoPadding</li>
 *   <li>金鑰長度：128-bit</li>
 *   <li>IV 長度：96-bit</li>
 *   <li>Tag 長度：128-bit</li>
 * </ul>
 * <p>
 * 提供的主要功能：
 * <ul>
 *   <li>{@link #encrypt(String, String)}：將 UTF-8 編碼字串加密並輸出為十六進位字串。</li>
 *   <li>{@link #encrypt(byte[], String)}：將位元組陣列加密並輸出位元組陣列。</li>
 *   <li>{@link #decrypt(String, String)}：將十六進位字串解密回 UTF-8 字串。</li>
 *   <li>{@link #decrypt(byte[], String)}：將加密後的位元組陣列解密回原始位元組陣列。</li>
 *   <li>{@link #parseHexStringToBytes(String)} 與 {@link #parseBytesToHexString(byte[])}：進行位元組與十六進位字串之間的轉換。</li>
 * </ul>
 *
 * <h3>實作細節</h3>
 * <ul>
 *   <li>金鑰由輸入的密碼字串經 {@link SecureRandom}（SHA1PRNG）種子化後，透過 {@link KeyGenerator} 產生。</li>
 *   <li>加密時隨機產生 IV，並將 IV 與密文組合輸出；解密時從輸入資料中分離 IV 與密文。</li>
 *   <li>所有加解密錯誤都會記錄至日誌。</li>
 * </ul>
 *
 * <h3>預期發展</h3>
 * 為了提升靈活性與可擴展性，未來可考慮：
 * <ul>
 *   <li>將加密演算法（AES、ChaCha20、DES 等）及模式/填充（GCM、CBC、PKCS5Padding 等）改為由外部傳入參數或透過設定檔指定，而非在程式碼中寫死。</li>
 *   <li>允許外部提供 {@link SecureRandom} 實例或自定義的金鑰衍生函數（KDF），例如 PBKDF2、scrypt、Argon2。</li>
 *   <li>支援可配置的金鑰長度（128、192、256-bit）。</li>
 *   <li>支援輸出 Base64 編碼格式，除了十六進位字串外。</li>
 *   <li>增加金鑰管理與安全儲存的整合，例如與 JCEKS 或雲端密鑰管理服務（KMS）結合。</li>
 * </ul>
 *
 * <h3>執行緒安全性</h3>
 * 本類別為 {@code @UtilityClass}（static 方法），沒有內部可變狀態；但 {@link Cipher} 等 JCE 元件在多執行緒間不可共用，
 * 每次加解密都會建立新的 {@link Cipher} 實例，因此是執行緒安全的。
 *
 * @author Leo
 * @since 2025.08.14 02:51:30
 */
@Slf4j
@UtilityClass
public class CryptUtil {
    private static final int GCM_TAG_LENGTH = 128/8;
    private static final int GCM_IV_LENGTH = 96/8;

    @Getter
    @Accessors(fluent = true)
    @ToString
    @AllArgsConstructor(access = AccessLevel.PACKAGE)
    private enum CRYPT_MODE {
        ENCRYPT(Cipher.ENCRYPT_MODE, "編碼"), DECRYPT(Cipher.DECRYPT_MODE, "解碼");

        private final int mode;
        private final String description;
    }

    public static String encrypt(String value, String mask) {
        byte[] bytes = value.getBytes(StandardCharsets.UTF_8);
        bytes = encrypt(bytes, mask);
        return parseBytesToHexString(bytes);
    }

    public static byte[] encrypt(byte[] content, String password) {
        return crypt(content, password, CRYPT_MODE.ENCRYPT);
    }

    public static String decrypt(String value, String mask) {
        byte[] bytes = parseHexStringToBytes(value);
        bytes = decrypt(bytes, mask);
        return new String(bytes, StandardCharsets.UTF_8);
    }

    public static byte[] decrypt(byte[] content, String password) {
        return crypt(content, password, CRYPT_MODE.DECRYPT);
    }

    private static byte[] crypt(byte[] content, String password, CRYPT_MODE cryptMode) {
        byte[] result = new byte[0];
        try {
            result = encodeDecodeImpl(content, password, cryptMode);
        }catch (GeneralSecurityException ex) {
            log.error("{}失敗：{}", cryptMode.description(), ex.getMessage(), ex);
        }
        return result;
    }

    private static byte[] encodeDecodeImpl(byte[] value, String password, CRYPT_MODE cryptMode) throws NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeyException, IllegalBlockSizeException, BadPaddingException, InvalidAlgorithmParameterException {
		/*
		SecretKey secretKey = getKey(password);
		byte[] encodeFormat = secretKey.getEncoded();
		SecretKey key = new SecretKeySpec(encodeFormat, "AES");
		Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");	//Cipher cipher = Cipher.getInstance("AES");
		cipher.init(cryptMode.mode(), key);
		return cipher.doFinal(value);
		*/

        SecretKey secretKey = getKey(password);
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        byte[] result;
        switch (cryptMode) {
            case ENCRYPT:
                result = encodeImpl(value, cipher, secretKey);
                break;
            case DECRYPT:
                result = decodeImpl(value, cipher, secretKey);
                break;
            default:
                throw new IllegalArgumentException("Unexpected cryptMode: " + cryptMode);
        }
        return result;
    }

    private static byte[] encodeImpl(byte[] value, Cipher cipher, SecretKey secretKey) throws InvalidAlgorithmParameterException, IllegalBlockSizeException, BadPaddingException, InvalidKeyException {
        byte[] iv = getInitializationVector(GCM_IV_LENGTH);
        AlgorithmParameterSpec parameterSpec = new GCMParameterSpec(GCM_TAG_LENGTH*8, iv);
        byte[] encryptedContent = cryptImpl(value, CRYPT_MODE.ENCRYPT, cipher, secretKey, parameterSpec);
        return mergeContentWithIv(encryptedContent, iv);
    }

    private static byte[] decodeImpl(byte[] value, Cipher cipher, SecretKey secretKey) throws InvalidAlgorithmParameterException, IllegalBlockSizeException, BadPaddingException, InvalidKeyException {
        byte[] iv = getInitializationVector(value, GCM_IV_LENGTH);
        AlgorithmParameterSpec parameterSpec = new GCMParameterSpec(GCM_TAG_LENGTH*8, iv);
        byte[] encryptedContent = splitContent(value, iv);
        return cryptImpl(encryptedContent, CRYPT_MODE.DECRYPT, cipher, secretKey, parameterSpec);
    }

    private static byte[] getInitializationVector(int initializationVectorLength) {
        byte[] initializationVector = new byte[initializationVectorLength];
        Random secureRandom = new SecureRandom();
        secureRandom.nextBytes(initializationVector);
        return initializationVector;
    }

    private static byte[] getInitializationVector(byte[] value, int initializationVectorLength) {
        return Arrays.copyOfRange(value, 0, initializationVectorLength);
    }

    private static byte[] mergeContentWithIv(byte[] value, byte[] iv) {
        byte[] contentWithIv = new byte[iv.length+value.length];
        System.arraycopy(iv, 0, contentWithIv, 0, iv.length);
        System.arraycopy(value, 0, contentWithIv, iv.length, value.length);
        return contentWithIv;
    }

    private static byte[] splitContent(byte[] value, byte[] iv) {
        return Arrays.copyOfRange(value, iv.length, value.length);
    }

    private static byte[] cryptImpl(byte[] value, CRYPT_MODE cryptMode, Cipher cipher, SecretKey secretKey, AlgorithmParameterSpec parameterSpec) throws InvalidAlgorithmParameterException, InvalidKeyException, IllegalBlockSizeException, BadPaddingException {
        cipher.init(cryptMode.mode(), secretKey, parameterSpec);
        return cipher.doFinal(value);
    }

    public static byte[] parseHexStringToBytes(String hexString) {
        if(hexString.length()%2!=0) {throw new IllegalArgumentException("Illegal Binary Stream!!! Hex string length must be even.");}
        byte[] result = new byte[hexString.length()/2];
        for(int i=0; i<hexString.length()/2; i++) {
            int high=Integer.parseInt(hexString.substring(i*2, i*2+1), 16);
            int low=Integer.parseInt(hexString.substring(i*2+1, i*2+2), 16);
            result[i]=(byte)(high*16+low);
        }
        return result;
    }

    public static String parseBytesToHexString(byte[] buffer) {
        StringBuilder stringBuilder = new StringBuilder();
        for(int i=0; i<buffer.length; i++) {
            String hex = String.format("%02X", buffer[i]&0xFF);
            stringBuilder.append(hex.toUpperCase());
        }
        return stringBuilder.toString();
    }

    private static SecretKey getKey(String keyString) throws NoSuchAlgorithmException {
        KeyGenerator keyGenerator = KeyGenerator.getInstance("AES");
        SecureRandom secureRandom = SecureRandom.getInstance("SHA1PRNG");
        secureRandom.setSeed(keyString.getBytes());
        keyGenerator.init(128, secureRandom);
        return keyGenerator.generateKey();
    }

}
