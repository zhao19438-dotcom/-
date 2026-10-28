package com.cainiao.forum.model;

import lombok.Data;

import java.util.Date;

@Data
// 敏感词，对应 t_sensitive_word 表；词库在内存中构建 DFA，改动后需刷新。
public class SensitiveWord {
    // 主键。
    private Long id;

    // 敏感词内容。
    private String word;

    // 创建时间。
    private Date createTime;
}
