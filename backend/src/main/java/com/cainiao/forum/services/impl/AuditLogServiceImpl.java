package com.cainiao.forum.services.impl;

import com.cainiao.forum.common.Constants;
import com.cainiao.forum.common.PageResult;
import com.cainiao.forum.dao.AuditLogMapper;
import com.cainiao.forum.model.AuditLog;
import com.cainiao.forum.services.IAuditLogService;
import com.cainiao.forum.utils.StringUtil;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.Date;
import java.util.List;

@Service
// 审计日志业务实现。
public class AuditLogServiceImpl implements IAuditLogService {

    @Resource
    private AuditLogMapper auditLogMapper;

    @Override
    public void log(Long userId, String username, String action,
                    String targetType, String targetId, String detail, String ip) {
        AuditLog row = new AuditLog();
        row.setUserId(userId);
        row.setUsername(username);
        row.setAction(action);
        row.setTargetType(targetType);
        row.setTargetId(targetId);
        // 详情最长 500，超长截断，避免写入失败。
        if (detail != null && detail.length() > 500) {
            detail = detail.substring(0, 500);
        }
        row.setDetail(detail);
        row.setIp(ip);
        row.setCreateTime(new Date());
        auditLogMapper.insert(row);
    }

    @Override
    public PageResult<AuditLog> list(String keyword, Integer page, Integer size) {
        int p = (page == null || page < 1) ? Constants.DEFAULT_PAGE : page;
        int s = (size == null || size < 1) ? Constants.DEFAULT_PAGE_SIZE : Math.min(size, Constants.MAX_PAGE_SIZE);
        if (keyword != null) {
            keyword = keyword.trim();
            if (keyword.isEmpty()) {
                keyword = null;
            }
        }
        List<AuditLog> records = auditLogMapper.selectPage(keyword, (p - 1) * s, s);
        int total = auditLogMapper.countAll(keyword);
        return PageResult.of(records, total, p, s);
    }
}
