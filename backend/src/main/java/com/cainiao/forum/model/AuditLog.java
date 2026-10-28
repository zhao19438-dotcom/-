package com.cainiao.forum.model;

import lombok.Data;

import java.util.Date;

@Data
// 审计日志，对应 t_audit_log 表；记录后台关键操作与登录事件。
public class AuditLog {
    // 主键。
    private Long id;

    // 操作人 ID。
    private Long userId;

    // 操作人用户名。
    private String username;

    // 动作，例如 "POST /admin/article/delete"。
    private String action;

    // 目标类型：user / article / board / report 等。
    private String targetType;

    // 目标 ID。
    private String targetId;

    // 操作详情（已脱敏的参数摘要）。
    private String detail;

    // 操作 IP。
    private String ip;

    // 操作时间。
    private Date createTime;
}
