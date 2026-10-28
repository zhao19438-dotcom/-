package com.cainiao.forum.biz.impl;

import com.cainiao.forum.biz.BizHandler;
import com.cainiao.forum.model.Article;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 默认业务处理器：普通帖子，不做任何扩展处理。
 * 同时作为未知业务类型的兜底，保证核心流程永不报错。
 */
@Component
public class ForumBizHandler implements BizHandler {

    @Override
    public String bizType() {
        return "forum";
    }
}
