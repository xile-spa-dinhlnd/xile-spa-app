package com.xilespa.common.util;

import java.text.Normalizer;
import java.util.regex.Pattern;

/** Chuẩn hóa chuỗi văn bản theo chuẩn chung của hệ thống (S3-08). */
public final class TextNormalizer {

    private static final Pattern WHITESPACE_TO_SPACE = Pattern.compile("[\\p{Z}\\s\\u0085]+");
    private static final Pattern INVISIBLE_PATTERN =
            Pattern.compile("[\\p{Cf}\\p{Cc}\\u115F\\u1160\\u3164\\uFFA0\\u2800]");
    private static final Pattern WHITESPACE_COLLAPSE = Pattern.compile(" +");

    private TextNormalizer() {}

    /**
     * Chuẩn hóa tên theo bốn bước: 1. Unicode NFC; 2. Đổi mọi khoảng trắng Unicode (\\p{Z}, \\s,
     * \\u0085) thành một khoảng trắng thường; 3. Bỏ lặng lẽ ký tự vô hình (\\p{Cf}, \\p{Cc}, Hangul
     * filler, Braille blank); 4. Gộp khoảng trắng liên tiếp và cắt đầu cuối.
     */
    public static String normalizeName(String name) {
        if (name == null) {
            return "";
        }
        String nfc = Normalizer.normalize(name, Normalizer.Form.NFC);
        String spacesReplaced = WHITESPACE_TO_SPACE.matcher(nfc).replaceAll(" ");
        String withoutInvisible = INVISIBLE_PATTERN.matcher(spacesReplaced).replaceAll("");
        return WHITESPACE_COLLAPSE.matcher(withoutInvisible).replaceAll(" ").trim();
    }
}
