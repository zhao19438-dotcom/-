package com.cainiao.forum.utils;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

/**
 * 密码工具类：新密码统一使用 BCrypt 存储（自带随机盐，抗暴力破解）。
 * 存量用户的 MD5+盐 密码在登录成功时自动升级为 BCrypt，无需批量重置。
 */
public class PasswordUtil {

    private static final BCryptPasswordEncoder ENCODER = new BCryptPasswordEncoder();

    private PasswordUtil() {
    }

    // 使用 BCrypt 加密明文密码，输出形如 $2a$10$... 的 60 位哈希。
    public static String encode(String rawPassword) {
        return ENCODER.encode(rawPassword);
    }

    // 校验明文密码与 BCrypt 哈希是否匹配。
    public static boolean matches(String rawPassword, String encodedPassword) {
        if (rawPassword == null || encodedPassword == null) {
            return false;
        }
        return ENCODER.matches(rawPassword, encodedPassword);
    }

    // 判断密文是否已经是 BCrypt 格式，用于登录时区分新老密码。
    public static boolean isBCrypt(String encodedPassword) {
        return encodedPassword != null && encodedPassword.startsWith("$2");
    }

    // 校验存量 MD5+盐 密码，仅在迁移升级时使用。
    public static boolean matchesLegacyMD5(String rawPassword, String salt, String storedPassword) {
        if (rawPassword == null || storedPassword == null) {
            return false;
        }
        return MD5Util.md5Salt(rawPassword, salt).equalsIgnoreCase(storedPassword);
    }
}
