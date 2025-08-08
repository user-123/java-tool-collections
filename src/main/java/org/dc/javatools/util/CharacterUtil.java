package org.dc.javatools.util;

import org.dc.javatools.util.validation.asserts.Assert;

public class CharacterUtil {

    private CharacterUtil() {
        throw new IllegalStateException("Utility class");
    }

    public static boolean isLegalCodePoint(int codePoint) {
        return codePoint >= 0x0000 && codePoint <= 0x10FFFF;
    }

    public static void validateCodePoint(int codePoint) throws IllegalArgumentException {
        Assert.isTrue(isLegalCodePoint(codePoint), IllegalArgumentException.class, "codePoint must be in range [0x0000, 0x10FFFF], illegal codePoint: [%d]".formatted(codePoint));
    }

    public static boolean isLegalCharacter(int codePoint) {
        return isLegalCodePoint(codePoint) && codePoint <= 0xFFFF;
    }

    public static void validateCharacter(int codePoint) throws IllegalArgumentException {
        Assert.isTrue(isLegalCodePoint(codePoint), IllegalArgumentException.class, "codePoint must be in range 0x0000~0xFFFF], illegal codePoint: [%d]".formatted(codePoint));
    }

    public static boolean isAscii(int codePoint) {
        validateCharacter(codePoint);
        return codePoint < 128;
    }

    public static boolean isLetterOrDigit(char character) {
        return Character.isLetterOrDigit(character);
    }

    public static boolean isEnglishLetterOrDigit(char character) {
        return isAscii(character) && isLetterOrDigit(character);
    }

}
