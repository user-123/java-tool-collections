package org.dc.javatools.util;

import java.io.File;
import java.security.SecureRandom;
import java.util.Random;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

public class StringUtil {

    private StringUtil() {
        throw new IllegalStateException("Utility class");
    }

    public static boolean isNull(String value) {
        return value == null;
    }

    public static boolean isNotNull(String value) {
        return !isNull(value);
    }

    public static boolean isNullOrEmpty(String value) {
        return isNull(value) || value.isEmpty();
    }

    public static boolean isNotNullAndNotEmpty(String value) {
        return !isNullOrEmpty(value);
    }

    public static boolean isNullOrBlank(String value) {
        return isNullOrEmpty(value) || value.trim().isEmpty();
    }

    public static boolean isNotNullAndNotBlank(String value) {
        return !isNullOrBlank(value);
    }

    public static boolean isSame(String value1, String value2) {
        return value1 == value2 || isNotNull(value1) && value1.equals(value2);
    }

    public static boolean containsSame(String value1, String... values) {
        return Stream.of(values).anyMatch(value2 -> isSame(value1, value2));
    }

    public static boolean isSameIgnoreCase(String value1, String value2) {
        return value1 == value2 || isNotNull(value1) && value1.equalsIgnoreCase(value2);
    }

    public static boolean containsSameIgnoreCase(String value1, String... values) {
        return Stream.of(values).anyMatch(value2 -> isSameIgnoreCase(value1, value2));
    }

    public static boolean isDigits(String value) {
        return isNotNullAndNotBlank(value) && value.chars().allMatch(Character::isDigit);
    }

    public static boolean isAsciiChars(String value) {
        return isNotNullAndNotEmpty(value) && value.chars().allMatch(CharacterUtil::isAscii);
    }

    public static String trimSeparator(String value) {
        if (isNullOrBlank(value)) {
            return value;
        }
        if (value.length() == 1) {
            return value.charAt(0) == File.separatorChar ? "" : value;
        }
        boolean firstCharIsSeparator = value.charAt(0) == File.separatorChar;
        boolean lastCharIsSeparator = value.charAt(value.length() - 1) == File.separatorChar;
        return value.substring(firstCharIsSeparator ? 1 : 0, lastCharIsSeparator ? value.length() - 1 : value.length());
    }

    public static String generateSqlQueryPlaceholders(int quantity) {
        return IntStream.range(0, quantity).mapToObj(i -> "?").collect(Collectors.joining(", ", "(", ")"));
    }

    public static String addSqlWildcards(String value) {
        return '%' + (value == null ? "" : value.replace("%", "\\%")) + '%';
    }

    public static boolean hasNullOrBlank(String... values) {
        for (String value : values) {
            if (isNullOrBlank(value)) {
                return true;
            }
        }
        return false;
    }

    public static String formatNumberWithLeadingZeros(int number, int length) {
        return String.format("%0" + length + "d", number);
    }

    private static final String ALPHANUMERIC_CHARACTERS = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
    private static final Random randomGenerator = new SecureRandom();

    public static final String getAlphanumericString(int length) {
        return IntStream.range(0, length)
                .mapToObj(i -> String.valueOf(ALPHANUMERIC_CHARACTERS.charAt(randomGenerator.nextInt(ALPHANUMERIC_CHARACTERS.length()))))
                .collect(Collectors.joining());
    }

    public static boolean isMatchingRegex(String input, String regex) {
        return input != null && input.matches(regex);
    }
}
