package com.cainiao.forum.services;

import com.cainiao.forum.model.ArticleReply;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;



// 回复业务接口，负责创建回复和按帖子查询回复列表。
public interface IArticleReplyService {


    @Transactional
    // 创建回复，需要同时让帖子回复数加 1，所以使用事务。
    void create (ArticleReply articleReply);


    // 查询某个帖子下的所有回复；currentUserId 非空时为每条回复标记当前用户是否已点赞。
    List<ArticleReply> selectByArticleId (Long articleId, Long currentUserId);


    // 点赞回复：写入点赞记录并让回复点赞数加 1（幂等）。
    void thumbsUp (Long replyId, Long userId);


    // 取消回复点赞：删除点赞记录并让回复点赞数减 1（幂等）。
    void thumbsDown (Long replyId, Long userId);
}

