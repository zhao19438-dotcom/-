package com.cainiao.forum.utils;

import java.util.UUID;



// UUID 工具类，用来生成随机字符串。
public class UUIDUtil {


    public static String UUID_36 () {
        // 标准 UUID 长度为 36，包含中划线。
        return UUID.randomUUID().toString();
    }


    public static String UUID_32 () {
        // 去掉中划线后得到 32 位字符串，适合作为密码盐。
        return UUID.randomUUID().toString().replace("-", "");
    }
}

