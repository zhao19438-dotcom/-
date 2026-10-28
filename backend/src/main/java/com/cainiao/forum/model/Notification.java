package com.cainiao.forum.model;

import lombok.Data;

import java.util.Date;

@Data
// 通知视图对象，不直接对应单张表，用于展示“有人回复了你的帖子”等消息。
public class Notification {
    // 通知来源记录的 ID，格式为「类型-记录ID」（如 reply-3、like-5），跨类型唯一，供前端列表 key 使用。
    private String id;

    // 通知类型，例如 reply 表示回复通知。
    private String type;

    // 通知标题。
    private String title;

    // 通知正文。
    private String content;

    // 关联帖子 ID，点击通知时用于跳转帖子详情。
    private Long articleId;

    // 关联帖子的标题。
    private String articleTitle;

    // 通知产生时间。
    private Date createTime;

    // 触发通知的用户，例如回复者。
    private User actor;
}
