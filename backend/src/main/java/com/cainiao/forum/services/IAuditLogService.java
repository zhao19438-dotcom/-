package com.cainiao.forum.services;

import com.cainiao.forum.common.PageResult;
import com.cainiao.forum.model.AuditLog;

// 审计日志服务：记录后台关键操作与登录事件，并提供分页查询。
public interface IAuditLogService {

    // 记录一条日志。
    void log(Long userId, String username, String action, String targetType, String targetId, String detail, String ip);

    // 分页查询日志，keyword 为空表示不过滤。
    PageResult<AuditLog> list(String keyword, Integer page, Integer size);
}
