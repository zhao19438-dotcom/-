package com.cainiao.forum.dao;

import com.cainiao.forum.model.Article;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
// 收藏记录 Mapper：唯一索引(articleId,userId)保证一人一帖只有一条收藏记录。
public interface ArticleCollectMapper {

    // 插入收藏记录，insert ignore 保证重复收藏时返回 0 而不报错。
    int insertIgnore(@Param("articleId") Long articleId, @Param("userId") Long userId);

    // 删除收藏记录（取消收藏），返回受影响行数。
    int delete(@Param("articleId") Long articleId, @Param("userId") Long userId);

    // 查询某用户是否已收藏某帖，大于 0 表示已收藏。
    int countByArticleAndUser(@Param("articleId") Long articleId, @Param("userId") Long userId);

    // 分页查询某用户的收藏帖子列表，按收藏时间倒序。
    List<Article> selectCollectPage(@Param("userId") Long userId,
                                    @Param("offset") Integer offset,
                                    @Param("limit") Integer limit);

    // 统计某用户收藏的帖子总数。
    int countCollectByUserId(@Param("userId") Long userId);
}
