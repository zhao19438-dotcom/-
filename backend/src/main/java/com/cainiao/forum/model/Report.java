package com.cainiao.forum.model;

import lombok.Data;

import java.util.Date;

@Data
// 举报记录，对应 t_report 表。
public class Report {
    // 主键。
    private Long id;

    // 被举报帖子 ID。
    private Long articleId;

    // 被举报私信 ID（帖子举报时为空）。
    private Long messageId;

    // 举报人 ID。
    private Long reporterId;

    // 举报类型：0垃圾广告 1色情低俗 2辱骂攻击 3违法违规 4其他。
    private Byte type;

    // 举报补充说明。
    private String reason;

    // 处理状态：0待处理 1已处理 2已驳回。
    private Byte status;

    // 处理结果说明。
    private String handleResult;

    private Date createTime;

    private Date updateTime;

    // 以下为后台列表展示用的关联字段（非表字段，由 SQL join 填充）。
    private String articleTitle;

    // 被举报私信的内容（后台列表展示用）。
    private String messageContent;

    private String reporterName;
}
