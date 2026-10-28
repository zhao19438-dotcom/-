package com.cainiao.forum.model;

import lombok.Data;

import java.util.Date;

@Data
// 论坛版块实体类，对应数据库中的 t_board 表。
public class Board {
    // 版块主键 ID。
    private Long id;

    // 版块名称，例如 Java、MySQL。
    private String name;

    // 当前版块下的帖子数量。
    private Integer articleCount;

    // 排序值，数字越小越靠前。
    private Integer sort;

    // 版块状态：0 正常，1 禁用。
    private Byte state;

    // 逻辑删除标记：0 未删除，1 已删除。
    private Byte deleteState;

    // 创建时间。
    private Date createTime;

    // 最近更新时间。
    private Date updateTime;

    // 版块业务类型：forum 普通版块，lostfound 失物招领版块等；决定发帖走哪个业务处理器。
    private String bizType;
}
