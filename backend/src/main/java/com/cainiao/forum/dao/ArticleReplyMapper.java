package com.cainiao.forum.dao;

import com.cainiao.forum.model.ArticleReply;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
// 回复表 Mapper，负责回复 CRUD、回复列表和通知查询。
public interface ArticleReplyMapper {
    // 完整插入回复记录。
    int insert(ArticleReply row);

    // 选择性插入回复记录，非空字段才写入数据库。
    int insertSelective(ArticleReply row);

    // 根据主键查询回复。
    ArticleReply selectByPrimaryKey(Long id);

    // 选择性更新回复字段。
    int updateByPrimaryKeySelective(ArticleReply row);

    // 全字段更新回复记录。
    int updateByPrimaryKey(ArticleReply row);


    // 查询指定帖子下的回复列表，并带上回复用户信息。
    List<ArticleReply> selectByArticleId (@Param("articleId") Long articleId);

    // 回复点赞数原子加 1。
    int increaseLikeCount(@Param("id") Long id);

    // 回复点赞数原子减 1（不低于 0）。
    int decreaseLikeCount(@Param("id") Long id);
}
