package com.cainiao.forum.dao;

import com.cainiao.forum.model.SensitiveWord;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
// 敏感词表 Mapper。
public interface SensitiveWordMapper {

    // 查询全部敏感词。
    List<SensitiveWord> selectAll();

    // 只取词内容，供内存词库构建使用。
    List<String> selectAllWords();

    // 新增敏感词。
    int insertSelective(SensitiveWord row);

    // 删除敏感词。
    int deleteByPrimaryKey(Long id);
}
