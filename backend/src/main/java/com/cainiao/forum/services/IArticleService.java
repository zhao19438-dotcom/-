package com.cainiao.forum.services;

import com.cainiao.forum.common.PageResult;
import com.cainiao.forum.model.Article;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

// 帖子业务接口，定义发帖、分页查询、搜索、详情、修改、点赞、收藏和删帖。
public interface IArticleService {



    @Transactional
    // 发布新帖，需要同时维护用户发帖数和版块帖子数，所以使用事务。
    void create (Article article);


    // 前台帖子分页查询：boardId 可空表示全部版块，keyword 可空表示不搜索，orderBy 支持 latest(默认)/hot。
    PageResult<Article> selectPage (Long boardId, String keyword, String orderBy, Integer page, Integer size);


    // 查询某个用户发布过的帖子。
    List<Article> selectByUserId (Long userId);


    // 查询帖子详情：访问量原子加 1，并填充当前用户是否点赞/收藏标记。
    Article selectDetailById (Long id, Long currentUserId);
    // 检查帖子可读性，不增加阅读量，供评论等关联接口使用。
    Article requireReadable(Long id, Long currentUserId);


    // 只按主键查询帖子基础信息。
    Article selectById (Long id);


    // 修改帖子标题和正文。
    void modify (Long id, String title, String content);
    void modify(Long id, String title, String content, Long boardId, String extJson);


    // 点赞：基于 t_article_like 唯一索引去重，重复点赞幂等（不重复计数）。
    void thumbsUp (Long articleId, Long userId);


    // 取消点赞：删除点赞记录并原子减 1，未点赞时幂等。
    void thumbsDown (Long articleId, Long userId);


    // 收藏：基于 t_article_collect 唯一索引去重，重复收藏幂等。
    void collect (Long articleId, Long userId);


    // 取消收藏：删除收藏记录，未收藏时幂等。
    void uncollect (Long articleId, Long userId);


    // 分页查询当前用户的收藏帖子列表。
    PageResult<Article> selectCollectPage (Long userId, Integer page, Integer size);


    @Transactional
    // 逻辑删除帖子，并同步减少用户和版块的帖子数量。
    void deleteById (Long id);


    // 有新回复时，帖子回复数原子加 1。
    void addOneReplyCountById (Long id);

}
