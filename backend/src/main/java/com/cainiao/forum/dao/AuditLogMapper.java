package com.cainiao.forum.dao;

import com.cainiao.forum.model.AuditLog;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
// 审计日志表 Mapper。
public interface AuditLogMapper {

    // 写入一条审计日志。
    int insert(AuditLog row);

    // 分页查询日志，keyword 可匹配用户名、动作、详情。
    List<AuditLog> selectPage(@Param("keyword") String keyword,
                              @Param("offset") Integer offset,
                              @Param("limit") Integer limit);

    // 日志总数统计。
    int countAll(@Param("keyword") String keyword);
}
