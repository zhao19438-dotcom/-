package com.cainiao.forum.dao;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
// 回复点赞记录 Mapper：唯一索引(replyId,userId)保证一人一回复只有一条点赞记录。
public interface ArticleReplyLikeMapper {

    // 插入点赞记录，insert ignore 保证重复点赞时返回 0 而不报错。
    int insertIgnore(@Param("replyId") Long replyId, @Param("userId") Long userId);

    // 删除点赞记录（取消点赞），返回受影响行数。
    int delete(@Param("replyId") Long replyId, @Param("userId") Long userId);

    // 查询某用户是否已点赞某回复，大于 0 表示已点赞。
    int countByReplyAndUser(@Param("replyId") Long replyId, @Param("userId") Long userId);

    // 查询某用户在某帖子下已点赞的全部回复 ID，用于回复列表批量标记。
    List<Long> selectLikedReplyIds(@Param("articleId") Long articleId, @Param("userId") Long userId);
}
