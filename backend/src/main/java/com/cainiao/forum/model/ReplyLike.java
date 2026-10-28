package com.cainiao.forum.model;

import lombok.Data;

import java.util.Date;

@Data
// 回复点赞记录实体类，对应数据库中的 t_article_reply_like 表。
public class ReplyLike {

    // 记录主键 ID。
    private Long id;

    // 被点赞的回复 ID。
    private Long replyId;

    // 点赞用户 ID。
    private Long userId;

    // 点赞时间。
    private Date createTime;
}
