package com.cainiao.forum.common;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * 项目级校验常量：Controller 与 Service 共用同一份规则，避免两处重复定义后改一处漏一处。
 */
public class Constants {

    private Constants() {
    }

    // 帖子标题与正文长度限制。
    public static final int TITLE_MIN_LENGTH = 2;
    public static final int TITLE_MAX_LENGTH = 100;
    public static final int CONTENT_MIN_LENGTH = 1;
    public static final int CONTENT_MAX_LENGTH = 20000;

    // 回复内容长度限制，对应数据库 varchar(500)。
    public static final int REPLY_MIN_LENGTH = 1;
    public static final int REPLY_MAX_LENGTH = 500;

    // 后台版块名称与排序限制。
    public static final int BOARD_NAME_MIN_LENGTH = 2;
    public static final int BOARD_NAME_MAX_LENGTH = 50;
    public static final int BOARD_SORT_MIN = 0;
    public static final int BOARD_SORT_MAX = 9999;

    // 分页默认值与上限，前台和后台共用。
    public static final int DEFAULT_PAGE = 1;
    public static final int DEFAULT_PAGE_SIZE = 10;
    public static final int MAX_PAGE_SIZE = 50;

    // 密码长度限制。
    public static final int PASSWORD_MIN_LENGTH = 6;
    public static final int PASSWORD_MAX_LENGTH = 32;

    // 头像上传限制：单个文件最大 2MB。
    public static final long MAX_AVATAR_SIZE = 2 * 1024 * 1024L;

    // 头像允许的扩展名白名单，其余一律拒绝。
    public static final Set<String> AVATAR_EXTENSIONS =
            new HashSet<>(Arrays.asList("jpg", "jpeg", "png", "gif"));

    // 发帖图片上传限制：单个文件最大 5MB。
    public static final long MAX_POST_IMAGE_SIZE = 5 * 1024 * 1024L;

    // 发帖图片允许的扩展名白名单（与头像一致，独立声明便于日后分别调整）。
    public static final Set<String> POST_IMAGE_EXTENSIONS =
            new HashSet<>(Arrays.asList("jpg", "jpeg", "png", "gif"));
}
