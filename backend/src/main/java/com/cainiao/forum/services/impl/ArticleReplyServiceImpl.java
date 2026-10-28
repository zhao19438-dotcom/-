package com.cainiao.forum.services.impl;

import com.cainiao.forum.common.AppResult;
import com.cainiao.forum.common.Constants;
import com.cainiao.forum.common.ResultCode;
import com.cainiao.forum.dao.ArticleReplyLikeMapper;
import com.cainiao.forum.dao.ArticleReplyMapper;
import com.cainiao.forum.exception.ApplicationException;
import com.cainiao.forum.model.Article;
import com.cainiao.forum.model.ArticleReply;
import com.cainiao.forum.services.IArticleReplyService;
import com.cainiao.forum.services.IArticleService;
import com.cainiao.forum.services.ISensitiveWordService;
import com.cainiao.forum.utils.StringUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;


@Slf4j
@Service
// 回复业务实现类，负责回复内容校验、写入和帖子回复数维护。
public class ArticleReplyServiceImpl implements IArticleReplyService {

    // 回复内容长度限制，对应数据库 varchar(500)。
    private static final int REPLY_MIN_LENGTH = Constants.REPLY_MIN_LENGTH;
    private static final int REPLY_MAX_LENGTH = Constants.REPLY_MAX_LENGTH;

    @Resource
    private ArticleReplyMapper articleReplyMapper;

    @Resource
    private ArticleReplyLikeMapper articleReplyLikeMapper;

    @Resource
    private IArticleService articleService;

    @Resource
    private ISensitiveWordService sensitiveWordService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    // 创建回复后还要增加帖子回复数，两个操作要么都成功，要么都回滚。
    public void create(ArticleReply articleReply) {

        // 创建回复必须指定帖子 ID 和发帖用户 ID。
        if (articleReply == null || articleReply.getArticleId() == null
                || articleReply.getPostUserId() == null
                || articleReply.getArticleId() <= 0
                || articleReply.getPostUserId() <= 0) {

            log.warn(ResultCode.FAILED_PARAMS_VALIDATE.toString());

            throw new ApplicationException(AppResult.failed(ResultCode.FAILED_PARAMS_VALIDATE));
        }

        // 去除首尾空格后再做敏感词与 XSS 过滤。
        articleReply.setContent(StringUtil.trim(articleReply.getContent()));
        articleReply.setContent(StringUtil.cleanXss(sensitiveWordService.filter(articleReply.getContent())));

        if (!StringUtil.lengthBetween(articleReply.getContent(), REPLY_MIN_LENGTH, REPLY_MAX_LENGTH)) {

            log.warn(ResultCode.FAILED_PARAMS_VALIDATE.toString());

            throw new ApplicationException(AppResult.failed(ResultCode.FAILED_PARAMS_VALIDATE));
        }

        // 回复所属帖子必须存在且未删除。
        Article article = articleService.selectById(articleReply.getArticleId());
        if (article == null || article.getDeleteState() == 1) {
            throw new ApplicationException(AppResult.failed(ResultCode.FAILED_ARTICLE_NOT_EXISTS));
        }
        // 被禁用的帖子不能继续回复。
        if (article.getState() == 1) {
            throw new ApplicationException(AppResult.failed(ResultCode.FAILED_ARTICLE_BANNED));
        }

        // 楼中楼：replyId 非空时校验被回复的评论存在、未删除且属于同一帖子。
        if (articleReply.getReplyId() != null) {
            ArticleReply parentReply = articleReplyMapper.selectByPrimaryKey(articleReply.getReplyId());
            if (parentReply == null || parentReply.getDeleteState() == 1
                    || !parentReply.getArticleId().equals(articleReply.getArticleId())) {
                throw new ApplicationException(AppResult.failed(ResultCode.FAILED_PARAMS_VALIDATE));
            }
            // 记录被回复的用户，前端展示「回复 @昵称」。
            articleReply.setReplyUserId(parentReply.getPostUserId());
        } else {
            articleReply.setReplyId(null);
            articleReply.setReplyUserId(null);
        }
        // 初始化回复统计和状态字段。
        articleReply.setLikeCount(0);
        articleReply.setState((byte) 0);
        articleReply.setDeleteState((byte) 0);
        Date date = new Date();
        articleReply.setCreateTime(date);
        articleReply.setUpdateTime(date);

        // 插入回复记录。
        int row = articleReplyMapper.insertSelective(articleReply);
        if (row != 1) {

            log.warn(ResultCode.ERROR_SERVICES.toString());

            throw new ApplicationException(AppResult.failed(ResultCode.ERROR_SERVICES));
        }


        // 回复成功后，同步让帖子回复数加 1。
        articleService.addOneReplyCountById(articleReply.getArticleId());

        log.info("回复成功, article id = " + articleReply.getArticleId() + ", user id = " +
                articleReply.getPostUserId());

    }

    @Override
    public List<ArticleReply> selectByArticleId(Long articleId, Long currentUserId) {

        // 查询回复列表必须指定合法帖子 ID。
        if (articleId == null || articleId <= 0) {

            log.warn(ResultCode.FAILED_PARAMS_VALIDATE.toString());

            throw new ApplicationException(AppResult.failed(ResultCode.FAILED_PARAMS_VALIDATE));
        }

        // 通过扩展 SQL 查询回复和回复用户信息。
        List<ArticleReply> result = articleReplyMapper.selectByArticleId(articleId);

        // 登录用户查看时，批量填充每条回复的点赞状态，前端据此高亮点赞按钮。
        if (currentUserId != null && result != null && !result.isEmpty()) {
            List<Long> likedIds = articleReplyLikeMapper.selectLikedReplyIds(articleId, currentUserId);
            if (likedIds != null && !likedIds.isEmpty()) {
                Set<Long> likedSet = new HashSet<>(likedIds);
                for (ArticleReply reply : result) {
                    reply.setLiked(likedSet.contains(reply.getId()));
                }
            } else {
                for (ArticleReply reply : result) {
                    reply.setLiked(false);
                }
            }
        }

        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    // 回复点赞与计数两步必须在同一事务里，任一失败整体回滚。
    public void thumbsUp(Long replyId, Long userId) {

        // 校验回复存在且未删除，同时取出用于校验结果同步。
        ArticleReply reply = articleReplyMapper.selectByPrimaryKey(replyId);
        if (reply == null || reply.getDeleteState() == 1) {
            throw new ApplicationException(AppResult.failed(ResultCode.FAILED_PARAMS_VALIDATE));
        }

        // insert ignore 配合唯一索引：首次点赞插入成功返回 1，重复点赞返回 0（幂等，不重复计数）。
        int row = articleReplyLikeMapper.insertIgnore(replyId, userId);
        if (row == 1) {
            // 只有真正新增了点赞记录才把回复点赞数原子加 1。
            articleReplyMapper.increaseLikeCount(replyId);
        }
        log.info("回复点赞成功, reply id = " + replyId + ", user id = " + userId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void thumbsDown(Long replyId, Long userId) {

        // 校验回复存在且未删除。
        ArticleReply reply = articleReplyMapper.selectByPrimaryKey(replyId);
        if (reply == null || reply.getDeleteState() == 1) {
            throw new ApplicationException(AppResult.failed(ResultCode.FAILED_PARAMS_VALIDATE));
        }

        // 删除点赞记录：已点赞返回 1 并原子减 1，未点赞返回 0（幂等）。
        int row = articleReplyLikeMapper.delete(replyId, userId);
        if (row == 1) {
            articleReplyMapper.decreaseLikeCount(replyId);
        }
        log.info("取消回复点赞, reply id = " + replyId + ", user id = " + userId);
    }
}

