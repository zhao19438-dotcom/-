package com.cainiao.forum.model;

import lombok.Data;

import java.util.Date;

@Data
// 收藏记录实体类，对应数据库中的 t_article_collect 表。
public class ArticleCollect {

    // 记录主键 ID。
    private Long id;

    // 被收藏的帖子 ID。
    private Long articleId;

    // 收藏用户 ID。
    private Long userId;

    // 收藏时间。
    private Date createTime;
}
