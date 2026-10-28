package com.cainiao.forum.model;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.util.Date;

@Data
// 帖子实体类，对应数据库中的 t_article 表。
public class Article {
    // 帖子主键 ID。
    private Long id;

    // 所属版块 ID。
    private Long boardId;

    // 发帖用户 ID。
    private Long userId;

    // 帖子标题。
    private String title;


    // 浏览次数。
    private Integer visitCount;


    // 回复数量。
    private Integer replyCount;


    // 点赞数量。
    private Integer likeCount;

    @ApiModelProperty("置顶")
    // 是否置顶：0 否，1 是，管理员设置。
    private Byte topState;

    @ApiModelProperty("精华")
    // 是否精华：0 否，1 是，管理员设置。
    private Byte essenceState;

    // 帖子状态：0 正常，1 禁用。
    private Byte state;

    // 逻辑删除标记：0 未删除，1 已删除。
    private Byte deleteState;

    // 发帖时间。
    private Date createTime;

    // 最近更新时间。
    private Date updateTime;

    // 帖子正文内容。
    private String content;

    @ApiModelProperty("业务类型")
    // 业务类型：与 t_board.bizType 对应。forum=普通帖，lostfound=失物招领等扩展模块。
    private String bizType;

    @ApiModelProperty("业务扩展字段")
    // 业务扩展字段（JSON 文本），不同业务模块的专属字段放在这里，核心表结构保持稳定。
    private String extJson;

    @ApiModelProperty("是否作者")
    // 当前登录用户是否为帖子作者，详情页用它控制编辑和删除按钮。
    private Boolean own;

    @ApiModelProperty("当前用户是否已点赞")
    // 当前登录用户是否已对该帖点赞，详情页用它控制点赞按钮状态。
    private Boolean liked;

    @ApiModelProperty("当前用户是否已收藏")
    // 当前登录用户是否已收藏该帖，详情页用它控制收藏按钮状态。
    private Boolean collected;


    // 发帖用户信息，查询详情或列表时由 MyBatis 关联填充。
    private User user;


    // 所属版块信息，查询详情或列表时由 MyBatis 关联填充。
    private Board board;
}
