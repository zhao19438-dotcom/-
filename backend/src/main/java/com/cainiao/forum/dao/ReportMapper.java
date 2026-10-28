package com.cainiao.forum.dao;

import com.cainiao.forum.model.Report;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.Date;
import java.util.List;

@Mapper
// 举报表 Mapper。
public interface ReportMapper {

    // 新增举报记录。
    int insertSelective(Report row);

    // 后台分页查询举报列表，可按状态过滤。
    List<Report> selectPageForAdmin(@Param("status") Byte status,
                                    @Param("offset") Integer offset,
                                    @Param("limit") Integer limit);

    // 后台举报总数统计。
    int countAllForAdmin(@Param("status") Byte status);

    // 处理举报：更新状态与处理结果。
    int updateHandle(@Param("id") Long id,
                     @Param("status") Byte status,
                     @Param("handleResult") String handleResult,
                     @Param("updateTime") Date updateTime);

    // 按 ID 查询举报记录（处置联动制裁时需要定位违规内容与作者）。
    Report selectByPrimaryKey(@Param("id") Long id);

    // 统计同一用户对同一帖子的举报次数，用于防重复举报。
    int countByArticleAndUser(@Param("articleId") Long articleId,
                              @Param("reporterId") Long reporterId);

    // 同一用户对同一条私信的举报次数。
    int countByMessageAndUser(@Param("messageId") Long messageId,
                              @Param("reporterId") Long reporterId);
}
