package com.cainiao.forum.services;

import com.cainiao.forum.common.PageResult;
import com.cainiao.forum.model.Report;

// 举报服务：提交举报、后台分页查询、处理举报。
public interface IReportService {

    // 提交举报。
    void submit(Long articleId, Long reporterId, Byte type, String reason);

    // 提交私信举报：被举报对象是收到的站内信。
    void submitMessage(Long messageId, Long reporterId, Byte type, String reason);

    // 后台分页查询举报列表，status 为 null 表示不过滤状态。
    PageResult<Report> listForAdmin(Byte status, Integer page, Integer size);

    // 处理举报：status=1 已处理，status=2 已驳回。
    void handle(Long id, Byte status, String handleResult);

    // 处理举报并联动制裁违规方：action = none(仅处理) / warn(警告) / delete(删除违规内容) / ban(禁言违规用户)。
    void handleWithAction(Long id, Byte status, String handleResult, String action, Long operatorId);
}
