package com.cainiao.forum.model;

import lombok.Data;

import java.util.Date;

@Data
// 帖子回复实体类，对应数据库中的 t_article_reply 表。
public class ArticleReply {

    // 回复主键 ID。
    private Long id;


    // 被回复的帖子 ID。
    private Long articleId;


    // 发表回复的用户 ID。
    private Long postUserId;


    // 被回复的回复 ID，支持楼中楼；为空表示直接回复帖子。
    private Long replyId;


    // 被回复用户 ID，支持楼中楼，随 replyId 一并由 Service 解析写入。
    private Long replyUserId;


    // 回复正文。
    private String content;


    // 回复点赞数量。
    private Integer likeCount;


    // 当前用户是否已点赞该回复（仅回复列表接口对登录用户填充，不落库）。
    private Boolean liked;


    // 回复状态：0 正常，1 禁用。
    private Byte state;


    // 逻辑删除标记：0 未删除，1 已删除。
    private Byte deleteState;


    // 回复创建时间。
    private Date createTime;


    // 回复最近更新时间。
    private Date updateTime;


    // 回复作者信息，查询回复列表时由 MyBatis 关联填充。
    private User user;


    // 被回复用户信息（楼中楼展示「回复 @昵称」用），查询回复列表时由 MyBatis 关联填充。
    private User replyUser;
}
