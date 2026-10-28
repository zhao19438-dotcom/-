package com.cainiao.forum.services.impl;

import com.cainiao.forum.model.Article;
import com.cainiao.forum.model.Board;
import com.cainiao.forum.services.IArticleService;
import com.cainiao.forum.services.IBoardService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.List;

@SpringBootTest
// 帖子业务集成测试，验证发帖、查询、修改、点赞、删除和回复数维护。
class ArticleServiceImplTest {

    @Resource
    private IArticleService articleService;
    @Resource
    private IBoardService boardService;
    @Resource
    private ObjectMapper objectMapper;

    @Test
    @Transactional
    // 测试创建帖子；事务回滚可以避免测试数据长期留在数据库。
    void create() {
        List<Board> boards = boardService.selectByNum(1);
        Long boardId = (boards != null && !boards.isEmpty()) ? boards.get(0).getId() : 14L;
        Article article = new Article();
        article.setUserId(1L);
        article.setBoardId(boardId);
        article.setTitle("单元测试帖子标题");
        article.setContent("这是用于自动化单元测试的正文内容");
        articleService.create(article);
        System.out.println("发贴成功");
    }

    @Test
    // 测试查询全部帖子列表，并打印成 JSON 方便观察关联字段。
    void selectAll() throws JsonProcessingException {

        List<Article> articles = articleService.selectPage(null, null, null, 1, 10).getRecords();

        System.out.println(objectMapper.writeValueAsString(articles));
    }

    @Test
    // 测试按版块查询帖子列表。
    void selectAllByBoardId() throws JsonProcessingException {
        List<Board> boards = boardService.selectByNum(1);
        Long boardId = (boards != null && !boards.isEmpty()) ? boards.get(0).getId() : 14L;
        List<Article> articles = articleService.selectPage(boardId, null, null, 1, 10).getRecords();
        System.out.println(objectMapper.writeValueAsString(articles));
    }


    @Test
    // 测试帖子详情查询，同时会触发访问量加 1。
    void selectDetailById() throws JsonProcessingException {
        Article article = articleService.selectDetailById(1L, null);
        System.out.println(objectMapper.writeValueAsString(article));
    }

    @Test
    @Transactional
    // 测试修改帖子标题和正文。
    void modify() {
        articleService.modify(1l, "单元测试111", "测试内容111");
        System.out.println("更新成功");
    }

    @Test
    // 测试按主键查询帖子基础信息。
    void selectById() throws JsonProcessingException {
        Article article = articleService.selectById(1l);
        System.out.println(objectMapper.writeValueAsString(article));
    }

    @Test
    @Transactional
    // 测试帖子点赞数加 1。
    void thumbsUpById() {
        articleService.thumbsUp(1L, 1L);
        System.out.println("点赞成功");
    }

    @Test
    @Transactional
    // 测试逻辑删除帖子，并同步维护用户和版块发帖数。
    void deleteById() {
        List<Board> boards = boardService.selectByNum(1);
        Long boardId = (boards != null && !boards.isEmpty()) ? boards.get(0).getId() : 14L;
        Article article = new Article();
        article.setUserId(1L);
        article.setBoardId(boardId);
        article.setTitle("待删除的单元测试帖子");
        article.setContent("测试逻辑删除流程的正文内容");
        articleService.create(article);

        articleService.deleteById(article.getId());
        System.out.println("删除成功");
    }

    @Test
    @Transactional
    // 测试帖子回复数加 1。
    void addOneReplyCountById() {
        articleService.addOneReplyCountById(1l);
        System.out.println("更新成功");
    }

    @Test
    // 测试查询指定用户发布的帖子列表。
    void selectByUserId() throws JsonProcessingException {
        List<Article> articles = articleService.selectByUserId(1l);
        System.out.println(objectMapper.writeValueAsString(articles));
    }
}
