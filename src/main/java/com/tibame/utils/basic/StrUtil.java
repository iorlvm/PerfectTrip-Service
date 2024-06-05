package com.tibame.utils.basic;

public class StrUtil {
    public static boolean isNotBlank(String str) {
        return str != null &&
                !str.trim().isEmpty();
    }

    public static boolean isBlank(String str) {
        return !isNotBlank(str);
    }
}
