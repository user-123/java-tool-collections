package org.dc.javatools.util;

import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPFile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * ※注意事項：此{@link FtpUtil}為參照{@link SftpUtil}由generative AI生成，設計邏輯可能與原本設計有所不同，使用之前請先驗證，謝謝。
 *
 * @implNote 此 util 邏輯僅針對低負載設計，預期全局僅使用單一 FTPClient 連線；<br />
 * 為了節省資源，若是沒有正在連線的 client，預設自動關閉 FTPClient，可經由 {@link #stopClientWhenIdle(boolean)} 調整設定。<br />
 * {@code FtpUtil#createConnection(String, int, String, String)} 新建連線時會實例化 FTPClient；<br />
 * 關閉連線時務必呼叫 {@code FtpUtil#disconnect(FTPClient)}<br />
 * TODO 在應用停止時呼叫 {@link #closeClient()} 以避免資源泄漏
 * TODO FTPClient沒有直接或對外暴露client和session的概念，所以ftpClient為冗餘，需移除，這部份必須要調整，如果有時間的話
 */
public class FtpUtil {
	private static FTPClient ftpClient;
	private static final Set<FTPClient> activeClients = new HashSet<>();
	private static boolean wouldStopClientWhenIdle = true;
	private static final Logger log = LoggerFactory.getLogger(FtpUtil.class);

	private FtpUtil() {
		throw new IllegalStateException("Utility class");
	}

	private static synchronized void initClient() throws IOException {
		if (isClientAlive()) {
			return;
		}
		startClient();
	}

	/**
	 * 設定閒置時（當沒有正在使用的 FTPClient 連線時）是否關閉 FTPClient
	 * @param enableStop {@code true} 啟用，{@code false} 停用
	 */
	public static void stopClientWhenIdle(boolean enableStop) {
		wouldStopClientWhenIdle = enableStop;
	}

	/**
	 * 建立 FTP 連線
	 *
	 * @param server FTP 伺服器 IP 或主機名
	 * @param port   連接埠
	 * @param user   帳號
	 * @param pwd    密碼
	 * @return 返回連線後的 FTPClient
	 * @throws IOException
	 */
	public static FTPClient createConnection(String server, int port, String user, String pwd) throws IOException {
		initClient();
		try {
			if (!ftpClient.isConnected()) {
				ftpClient.connect(server, port);
				log.info("FTP 已連線至 {}:{}", server, port);
				// 可加入等待連線穩定的邏輯，例如 sleep 或連線超時設定
			}
			boolean loginSuccess = ftpClient.login(user, pwd);
			if (!loginSuccess) {
				throw new IOException("FTP 登入失敗，使用者：" + user);
			}
			// 設定二進位檔案模式
			ftpClient.setFileType(FTPClient.BINARY_FILE_TYPE);
			// 若需要設定被動模式，可啟用下面這行
			// ftpClient.enterLocalPassiveMode();
			activeClients.add(ftpClient);
			return ftpClient;
		} catch (IOException ex) {
			log.error("FTP 連線錯誤，ip：{}:{}, 帳號：{}", server, port, user, ex);
			throw (IOException) ex.fillInStackTrace();
		}
	}

	/**
	 * 斷開 FTP 連線
	 *
	 * @param client 使用中的 FTPClient
	 */
	public static void disconnect(FTPClient client) {
		if (client != null && client.isConnected()) {
			try {
				client.logout();
				client.disconnect();
				log.info("FTP 連線已斷開");
			} catch (IOException e) {
				log.error("FTP 斷線錯誤", e);
			}
		}
		activeClients.remove(client);
		if (wouldStopClientWhenIdle && activeClients.isEmpty()) {
			stopClient();
		}
	}

	/**
	 * 上傳檔案（以字串路徑）
	 */
	public static void uploadFile(String server, int port, String user, String pwd,
								  String localFilePath, String remoteFilePath) throws IOException {
		uploadFile(server, port, user, pwd, Paths.get(localFilePath), remoteFilePath);
	}

	/**
	 * 上傳檔案（本機路徑為 Path 物件）
	 */
	public static void uploadFile(String server, int port, String user, String pwd,
                                  Path localFilePath, String remoteFilePath) throws IOException {
		FTPClient client = null;
		try {
			client = createConnection(server, port, user, pwd);
			uploadFile(client, localFilePath, remoteFilePath);
		} finally {
			disconnect(client);
		}
	}

	/**
	 * 下載檔案（以字串路徑）
	 */
	public static void downloadFile(String server, int port, String user, String pwd,
									String remoteFilePath, String localFilePath) throws IOException {
		downloadFile(server, port, user, pwd, remoteFilePath, Paths.get(localFilePath));
	}

	/**
	 * 下載檔案（本機路徑為 Path 物件）
	 */
	public static void downloadFile(String server, int port, String user, String pwd,
									String remoteFilePath, Path localFilePath) throws IOException {
		FTPClient client = null;
		try {
			client = createConnection(server, port, user, pwd);
			downloadFile(client, remoteFilePath, localFilePath);
		} finally {
			disconnect(client);
		}
	}

	/**
	 * 上傳檔案（提供已連線的 FTPClient）
	 */
	public static void uploadFile(FTPClient client, String localFilePath, String remoteFilePath) throws IOException {
		uploadFile(client, Paths.get(localFilePath), remoteFilePath);
	}

	/**
	 * 下載檔案（提供已連線的 FTPClient）
	 */
	public static void downloadFile(FTPClient client, String remoteFilePath, String localFilePath) throws IOException {
		downloadFile(client, remoteFilePath, Paths.get(localFilePath));
	}

	/**
	 * 上傳檔案（提供已連線的 FTPClient，本機路徑為 Path 物件）
	 */
	public static void uploadFile(FTPClient client, Path localFilePath, String remoteFilePath) throws IOException {
		if (!isClientAlive(client)) {
			throw new IOException("FTPClient 尚未連線或已關閉");
		}
		try (InputStream input = Files.newInputStream(localFilePath)) {
			// 若遠端目錄不存在則嘗試建立
			String remoteDir = remoteFilePath.substring(0, remoteFilePath.lastIndexOf('/'));
			createRemoteDirectories(client, remoteDir);
			boolean done = client.storeFile(remoteFilePath, input);
			if (!done) {
				throw new IOException("上傳檔案失敗: " + localFilePath);
			}
			// 如需上傳後刪除本機檔案，可啟用下行
			// Files.delete(localFilePath);
			log.info("已上傳檔案 {} 至遠端 {}", localFilePath, remoteFilePath);
		} catch (IOException ex) {
			log.error("FTP 上傳檔案錯誤：{}", ex.getMessage(), ex);
			throw (IOException) ex.fillInStackTrace();
		}
	}

	/**
	 * 下載檔案（提供已連線的 FTPClient，本機路徑為 Path 物件）
	 */
	public static void downloadFile(FTPClient client, String remoteFilePath, Path localFilePath) throws IOException {
		if (!isClientAlive(client)) {
			throw new IOException("FTPClient 尚未連線或已關閉");
		}
		try (OutputStream output = Files.newOutputStream(localFilePath)) {
			// 確保本機目錄存在
			Files.createDirectories(localFilePath.getParent());
			boolean done = client.retrieveFile(remoteFilePath, output);
			if (!done) {
				throw new IOException("下載檔案失敗: " + remoteFilePath);
			}
			// 如需下載後刪除遠端檔案，可啟用下行
			// client.deleteFile(remoteFilePath);
			log.info("已下載遠端檔案 {} 至本機 {}", remoteFilePath, localFilePath);
		} catch (IOException ex) {
			log.error("FTP 下載檔案錯誤：{}", ex.getMessage(), ex);
			throw (IOException) ex.fillInStackTrace();
		}
	}

	/**
	 * 上傳多個檔案
	 *
	 * @param client         已連線的 FTPClient
	 * @param localFilePaths 本機檔案清單
	 * @param remoteFilePaths 遠端檔案路徑清單（必須和本機檔案清單順序一一對應）
	 * @throws IOException
	 */
	public static void uploadFiles(FTPClient client, List<Path> localFilePaths, List<String> remoteFilePaths) throws IOException {
		if (localFilePaths == null || remoteFilePaths == null ||
				localFilePaths.isEmpty() || remoteFilePaths.isEmpty() ||
				localFilePaths.size() != remoteFilePaths.size()) {
			throw new IllegalArgumentException("檔案清單錯誤");
		}
		for (int i = 0; i < localFilePaths.size(); i++) {
			uploadFile(client, localFilePaths.get(i), remoteFilePaths.get(i));
		}
	}

	/**
	 * 下載多個檔案
	 *
	 * @param client         已連線的 FTPClient
	 * @param remoteFilePaths 遠端檔案路徑清單
	 * @param localFilePaths 本機檔案清單（必須和遠端檔案清單順序一一對應）
	 * @throws IOException
	 */
	public static void downloadFiles(FTPClient client, List<String> remoteFilePaths, List<Path> localFilePaths) throws IOException {
		if (localFilePaths == null || remoteFilePaths == null ||
				localFilePaths.isEmpty() || remoteFilePaths.isEmpty() ||
				localFilePaths.size() != remoteFilePaths.size()) {
			throw new IllegalArgumentException("檔案清單錯誤");
		}
		for (int i = 0; i < remoteFilePaths.size(); i++) {
			downloadFile(client, remoteFilePaths.get(i), localFilePaths.get(i));
		}
	}

	/**
	 * 列出遠端目錄中的檔案名稱（字串清單）
	 *
	 * @param client        已連線的 FTPClient
	 * @param remoteDirPath 遠端目錄路徑
	 * @return 檔案名稱清單
	 * @throws IOException
	 */
	public static List<String> listRemoteFilesAsStrings(FTPClient client, String remoteDirPath) throws IOException {
		if (!isClientAlive(client)) {
			throw new IOException("FTPClient 尚未連線或已關閉");
		}
		FTPFile[] files = client.listFiles(remoteDirPath);
		List<String> fileNames = Arrays.stream(files)
				.filter(FTPFile::isFile)
				.map(FTPFile::getName)
				.collect(Collectors.toList());
		log.info("遠端目錄[{}]包含檔案：{}", remoteDirPath, fileNames);
		return fileNames;
	}

	private static void startClient() {
		ftpClient = new FTPClient();
		// 可根據需要設定逾時時間
		ftpClient.setConnectTimeout((int) TimeUnit.SECONDS.toMillis(5));
		ftpClient.setDefaultTimeout((int) TimeUnit.SECONDS.toMillis(5));
		log.trace("實例化 FTPClient");
	}

	private static void stopClient() {
		if (ftpClient != null && ftpClient.isConnected()) {
			try {
				ftpClient.disconnect();
				log.trace("FTPClient 已停止");
			} catch (IOException e) {
				log.error("停止 FTPClient 發生錯誤", e);
			}
		}
	}

	/**
	 * 關閉並釋放 FTPClient 資源
	 *
	 * @throws IOException
	 */
	public static synchronized void closeClient() throws IOException {
		if (ftpClient != null && ftpClient.isConnected()) {
			ftpClient.disconnect();
		}
		ftpClient = null;
	}

	private static boolean isClientAlive() {
		return ftpClient != null && ftpClient.isConnected();
	}

	private static boolean isClientAlive(FTPClient client) {
		return client != null && client.isConnected();
	}

	/**
	 * 遞迴建立遠端目錄（若不存在則建立）
	 *
	 * @param client        已連線的 FTPClient
	 * @param remoteDirPath 遠端目錄路徑（例如：/upload/files）
	 * @throws IOException
	 */
	private static void createRemoteDirectories(FTPClient client, String remoteDirPath) throws IOException {
		String[] dirs = remoteDirPath.split("/");
		if (dirs.length == 0) return;
		String path = "";
		// 若路徑以 / 開頭則保留
		if (remoteDirPath.startsWith("/")) {
			path = "/";
		}
		for (String dir : dirs) {
			if (dir.isEmpty()) {
				continue;
			}
			if (!path.endsWith("/")) {
				path += "/";
			}
			path += dir;
			if (!client.changeWorkingDirectory(path)) {
				boolean created = client.makeDirectory(path);
				if (created) {
					log.info("已建立遠端目錄：{}", path);
				} else {
					log.error("建立遠端目錄失敗：{}", path);
					throw new IOException("建立遠端目錄失敗：" + path);
				}
			}
		}
	}
}
