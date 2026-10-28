package com.cainiao.forum.dao;

import com.cainiao.forum.model.LostFound;
import org.apache.ibatis.annotations.Mapper;

@Mapper
// 失物招领扩展表 Mapper。
public interface LostFoundMapper {
    // 写入一条失物招领扩展记录。
    int insert(LostFound row);

    // 按帖子 ID 查询扩展信息。
    LostFound selectByArticleId(Long articleId);
    int updateDetails(LostFound row);
    int deleteByArticleId(Long articleId);
}
