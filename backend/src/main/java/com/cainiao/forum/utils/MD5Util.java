package com.cainiao.forum.utils;

import org.apache.commons.codec.digest.DigestUtils;



// MD5 加密工具类，主要用于密码摘要计算。
public class MD5Util {


    public static String md5 (String str) {
        // md5Hex 会把字符串计算成 32 位十六进制摘要。
        return DigestUtils.md5Hex(str);
    }


    public static String md5Salt (String str, String salt) {
        // 先对原文加密，再拼接盐二次加密，降低相同密码得到相同密文的风险。
        return md5(md5(str) + salt);
    }
}

