package com.cainiao.forum.dao;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
// 点赞记录 Mapper：唯一索引(articleId,userId)保证一人一帖只有一条点赞记录。
public interface ArticleLikeMapper {

    // 插入点赞记录，insert ignore 保证重复点赞时返回 0 而不报错。
    int insertIgnore(@Param("articleId") Long articleId, @Param("userId") Long userId);

    // 删除点赞记录（取消点赞），返回受影响行数。
    int delete(@Param("articleId") Long articleId, @Param("userId") Long userId);

    // 查询某用户是否已点赞某帖，大于 0 表示已点赞。
    int countByArticleAndUser(@Param("articleId") Long articleId, @Param("userId") Long userId);
}
