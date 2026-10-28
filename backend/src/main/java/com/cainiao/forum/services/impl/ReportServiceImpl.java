package com.cainiao.forum.services.impl;

import com.cainiao.forum.common.AppResult;
import com.cainiao.forum.common.Constants;
import com.cainiao.forum.common.PageResult;
import com.cainiao.forum.common.ResultCode;
import com.cainiao.forum.dao.ArticleMapper;
import com.cainiao.forum.dao.MessageMapper;
import com.cainiao.forum.dao.ReportMapper;
import com.cainiao.forum.dao.UserMapper;
import com.cainiao.forum.exception.ApplicationException;
import com.cainiao.forum.model.Article;
import com.cainiao.forum.model.Message;
import com.cainiao.forum.model.Report;
import com.cainiao.forum.model.User;
import com.cainiao.forum.services.IReportService;
import com.cainiao.forum.utils.StringUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.Date;
import java.util.List;

@Slf4j
@Service
// 举报业务实现。
public class ReportServiceImpl implements IReportService {

    @Resource
    private ReportMapper reportMapper;

    @Resource
    private ArticleMapper articleMapper;

    @Resource
    private MessageMapper messageMapper;

    @Resource
    private UserMapper userMapper;

    @Override
    public void submit(Long articleId, Long reporterId, Byte type, String reason) {
        if (articleId == null || articleId <= 0 || reporterId == null || reporterId <= 0) {
            throw new ApplicationException(AppResult.failed(ResultCode.FAILED_PARAMS_VALIDATE));
        }
        if (type == null || type < 0 || type > 4) {
            type = 4;
        }
        reason = StringUtil.trim(reason);
        if (reason != null && reason.length() > 500) {
            throw new ApplicationException(AppResult.failed("举报说明不能超过 500 字"));
        }

        // 被举报帖子必须存在。
        Article article = articleMapper.selectByPrimaryKey(articleId);
        if (article == null) {
            throw new ApplicationException(AppResult.failed(ResultCode.FAILED_ARTICLE_NOT_EXISTS));
        }

        // 同一用户对同一帖子只能举报一次，避免刷屏。
        int exists = reportMapper.countByArticleAndUser(articleId, reporterId);
        if (exists > 0) {
            throw new ApplicationException(AppResult.failed("您已举报过该帖，请等待管理员处理"));
        }

        Report row = new Report();
        row.setArticleId(articleId);
        row.setReporterId(reporterId);
        row.setType(type);
        row.setReason(reason);
        row.setStatus((byte) 0);
        Date date = new Date();
        row.setCreateTime(date);
        row.setUpdateTime(date);

        int r = reportMapper.insertSelective(row);
        if (r != 1) {
            throw new ApplicationException(AppResult.failed(ResultCode.FAILED_CREATE));
        }
        log.info("收到举报, articleId = " + articleId + ", reporterId = " + reporterId);
    }

    @Override
    public void submitMessage(Long messageId, Long reporterId, Byte type, String reason) {
        // 举报私信必须指定合法消息 ID。
        if (messageId == null || messageId <= 0 || reporterId == null || reporterId <= 0) {
            throw new ApplicationException(AppResult.failed(ResultCode.FAILED_PARAMS_VALIDATE));
        }
        if (type == null || type < 0 || type > 4) {
            type = 4;
        }
        reason = StringUtil.trim(reason);
        if (reason != null && reason.length() > 500) {
            throw new ApplicationException(AppResult.failed("举报说明不能超过 500 字"));
        }

        // 被举报私信必须真实存在。
        Message message = messageMapper.selectByPrimaryKey(messageId);
        if (message == null || message.getDeleteState() == 1) {
            throw new ApplicationException(AppResult.failed(ResultCode.FAILED_MESSAGE_NOT_EXISTS));
        }

        // 同一用户对同一条私信只能举报一次。
        int exists = reportMapper.countByMessageAndUser(messageId, reporterId);
        if (exists > 0) {
            throw new ApplicationException(AppResult.failed("您已举报过该私信，请等待管理员处理"));
        }

        Report row = new Report();
        row.setMessageId(messageId);
        row.setReporterId(reporterId);
        row.setType(type);
        row.setReason(reason);
        row.setStatus((byte) 0);
        Date date = new Date();
        row.setCreateTime(date);
        row.setUpdateTime(date);

        int r = reportMapper.insertSelective(row);
        if (r != 1) {
            throw new ApplicationException(AppResult.failed(ResultCode.FAILED_CREATE));
        }
        log.info("收到私信举报, messageId = " + messageId + ", reporterId = " + reporterId);
    }

    @Override
    public PageResult<Report> listForAdmin(Byte status, Integer page, Integer size) {
        int p = (page == null || page < 1) ? Constants.DEFAULT_PAGE : page;
        int s = (size == null || size < 1) ? Constants.DEFAULT_PAGE_SIZE : Math.min(size, Constants.MAX_PAGE_SIZE);
        if (status != null && (status < 0 || status > 2)) {
            status = null;
        }
        List<Report> records = reportMapper.selectPageForAdmin(status, (p - 1) * s, s);
        int total = reportMapper.countAllForAdmin(status);
        return PageResult.of(records, total, p, s);
    }

    @Override
    public void handle(Long id, Byte status, String handleResult) {
        if (id == null || id <= 0 || status == null || (status != 1 && status != 2)) {
            throw new ApplicationException(AppResult.failed(ResultCode.FAILED_PARAMS_VALIDATE));
        }
        handleResult = StringUtil.trim(handleResult);
        if (handleResult != null && handleResult.length() > 500) {
            throw new ApplicationException(AppResult.failed("处理结果不能超过 500 字"));
        }
        int row = reportMapper.updateHandle(id, status, handleResult, new Date());
        if (row != 1) {
            throw new ApplicationException(AppResult.failed(ResultCode.ERROR_SERVICES));
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    // 处置举报 + 联动制裁违规方，制裁失败时整体回滚，避免"举报单已处理但内容未处置"的不一致。
    public void handleWithAction(Long id, Byte status, String handleResult, String action, Long operatorId) {

        // 先按原逻辑更新举报单状态。
        handle(id, status, handleResult);

        // 驳回或仅处理时不需要联动制裁。
        if (status != 1 || action == null || "none".equals(action)) {
            return;
        }

        Report report = reportMapper.selectByPrimaryKey(id);
        if (report == null) {
            throw new ApplicationException(AppResult.failed(ResultCode.ERROR_SERVICES));
        }

        // 定位违规内容的作者：帖子举报取发帖人，私信举报取发信人。
        Long offenderId = null;
        String targetDesc;
        if (report.getArticleId() != null) {
            Article article = articleMapper.selectByPrimaryKey(report.getArticleId());
            if (article == null) {
                return;
            }
            offenderId = article.getUserId();
            targetDesc = "帖子《" + article.getTitle() + "》";

            // 删除违规内容：帖子改为禁用状态（保留数据，可由后台恢复）。
            if ("delete".equals(action)) {
                Article update = new Article();
                update.setId(article.getId());
                update.setState((byte) 1);
                update.setUpdateTime(new Date());
                articleMapper.updateByPrimaryKeySelective(update);
            }
        } else if (report.getMessageId() != null) {
            Message message = messageMapper.selectByPrimaryKey(report.getMessageId());
            if (message == null) {
                return;
            }
            offenderId = message.getPostUserId();
            targetDesc = "一条私信";

            // 删除违规内容：私信逻辑删除。
            if ("delete".equals(action)) {
                Message update = new Message();
                update.setId(message.getId());
                update.setDeleteState((byte) 1);
                update.setUpdateTime(new Date());
                messageMapper.updateByPrimaryKeySelective(update);
            }
        } else {
            return;
        }

        // 警告：以处理管理员身份给违规用户发一条系统警告私信。
        // 注意直接写库而不走 messageService.create，避免"对方回复前只能发一条"的会话限制拦截管理员的警告。
        if ("warn".equals(action) || "delete".equals(action)) {
            Message warn = new Message();
            warn.setPostUserId(operatorId);
            warn.setReceiveUserId(offenderId);
            String actionText = "delete".equals(action) ? "内容已被移除。" : "";
            warn.setContent("【违规警告】你发布的" + targetDesc + "经举报核实存在违规行为，"
                    + actionText + "请遵守社区规范，多次违规将被禁言。处理说明：" + (handleResult == null ? "无" : handleResult));
            warn.setState((byte) 0);
            warn.setDeleteState((byte) 0);
            Date now = new Date();
            warn.setCreateTime(now);
            warn.setUpdateTime(now);
            messageMapper.insertSelective(warn);
        }

        // 禁言：把违规用户状态改为禁言（state=1），其发帖/回复/私信将被拦截。
        if ("ban".equals(action)) {
            if (offenderId.equals(operatorId)) {
                throw new ApplicationException(AppResult.failed("不能对自己执行禁言操作"));
            }
            User update = new User();
            update.setId(offenderId);
            update.setState((byte) 1);
            update.setUpdateTime(new Date());
            userMapper.updateByPrimaryKeySelective(update);
            log.info("举报处置联动禁言, report id = " + id + ", offender id = " + offenderId);
        }
    }
}
