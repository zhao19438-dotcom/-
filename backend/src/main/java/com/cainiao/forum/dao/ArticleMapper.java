package com.cainiao.forum.dao;

import com.cainiao.forum.model.Article;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
// 帖子表 Mapper，负责 t_article 的基础 CRUD 和自定义查询。
public interface ArticleMapper {
    // 完整插入帖子记录。
    int insert(Article row);

    // 选择性插入帖子记录，非空字段才写入数据库。
    int insertSelective(Article row);

    // 根据帖子 ID 查询基础信息。
    Article selectByPrimaryKey(Long id);

    // 选择性更新帖子字段。
    int updateByPrimaryKeySelective(Article row);

    // 更新包含正文 content 的帖子记录。
    int updateByPrimaryKeyWithBLOBs(Article row);

    // 全字段更新帖子记录。
    int updateByPrimaryKey(Article row);


    // 前台帖子分页查询：boardId 与 keyword 均可选，orderBy 支持 latest(默认)/hot。
    List<Article> selectPage (@Param("boardId") Long boardId,
                              @Param("keyword") String keyword,
                              @Param("orderBy") String orderBy,
                              @Param("offset") Integer offset,
                              @Param("limit") Integer limit);

    // 前台帖子总数统计，条件与 selectPage 一致。
    int countPage (@Param("boardId") Long boardId,
                   @Param("keyword") String keyword);


    // 查询帖子详情，包含作者和版块信息。
    Article selectDetailById (@Param("id") Long id);


    // 查询某个用户发布的帖子。
    List<Article> selectByUserId (@Param("userId") Long userId);


    // 后台帖子管理分页查询，可按标题关键字搜索。
    List<Article> selectAllForAdmin(@Param("keyword") String keyword,
                                    @Param("offset") Integer offset,
                                    @Param("limit") Integer limit);

    // 后台帖子管理统计总数。
    int countAllForAdmin(@Param("keyword") String keyword);


    // 访问量原子加 1。
    int increaseVisitCount(@Param("id") Long id);

    // 点赞数原子加 1。
    int increaseLikeCount(@Param("id") Long id);

    // 点赞数原子减 1。
    int decreaseLikeCount(@Param("id") Long id);

    // 回复数原子加 1。
    int increaseReplyCount(@Param("id") Long id);

    // 回复数原子减 1。
    int decreaseReplyCount(@Param("id") Long id);

    // 设置置顶状态。
    int updateTopState(Article row);

    // 设置精华状态。
    int updateEssenceState(Article row);

}

