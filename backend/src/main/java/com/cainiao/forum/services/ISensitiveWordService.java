package com.cainiao.forum.services;

import com.cainiao.forum.model.SensitiveWord;

import java.util.List;

// 敏感词服务：负责词库维护与内容过滤。
public interface ISensitiveWordService {

    // 重新加载词库到内存（后台改词后调用）。
    void refresh();

    // 过滤文本：命中敏感词的部分替换为等长 * 。
    String filter(String text);

    // 查询全部敏感词。
    List<SensitiveWord> listAll();

    // 新增敏感词。
    void add(String word);

    // 删除敏感词。
    void delete(Long id);
}
