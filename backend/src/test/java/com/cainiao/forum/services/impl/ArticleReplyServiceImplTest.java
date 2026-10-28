package com.cainiao.forum.services.impl;

import com.cainiao.forum.model.ArticleReply;
import com.cainiao.forum.services.IArticleReplyService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.List;

@SpringBootTest
// 回复业务集成测试，验证创建回复和按帖子查询回复列表。
class ArticleReplyServiceImplTest {

    @Resource
    private IArticleReplyService articleReplyService;
    @Resource
    private ObjectMapper objectMapper;

    @Test
    @Transactional
    // 测试创建回复；创建成功后帖子回复数也会加 1。
    void create() {

        ArticleReply articleReply = new ArticleReply();
        articleReply.setArticleId(1l);
        articleReply.setPostUserId(1l);
        articleReply.setContent("单元测试回复");

        articleReplyService.create(articleReply);
        System.out.println("回复成功");
    }

    @Test
    // 测试查询某个帖子下的回复列表，并打印关联用户信息。
    void selectByArticleId() throws JsonProcessingException {
        List<ArticleReply> articleReplies = articleReplyService.selectByArticleId(1L, null);
        System.out.println(objectMapper.writeValueAsString(articleReplies));

    }
}
