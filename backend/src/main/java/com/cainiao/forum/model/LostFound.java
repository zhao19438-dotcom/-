package com.cainiao.forum.model;

import lombok.Data;

import java.util.Date;

@Data
// 失物招领扩展信息，与 t_article 通过 articleId 一对一关联。
public class LostFound {
    // 主键。
    private Long id;

    // 关联帖子 ID。
    private Long articleId;

    // 类型：lost=丢失，found=拾得。
    private String lostType;

    // 物品名称。
    private String itemName;

    // 联系方式。
    private String contact;

    // 地点。
    private String location;

    // 状态：0 进行中，1 已解决。
    private Byte status;

    // 创建时间。
    private Date createTime;
}
