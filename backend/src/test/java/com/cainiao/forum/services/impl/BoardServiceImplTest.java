package com.cainiao.forum.services.impl;

import com.cainiao.forum.model.Board;
import com.cainiao.forum.services.IBoardService;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
// 版块业务集成测试，验证首页版块查询和帖子数量维护。
class BoardServiceImplTest {

    @Resource
    private IBoardService boardService;

    /**
     * 取一个当前存在的版块。
     * 测试不写死 ID：数据库里的版块会随运营调整，写死会导致测试随数据变化而失败。
     */
    private Board anyBoard() {
        List<Board> boards = boardService.selectByNum(1);
        assertFalse(boards == null || boards.isEmpty(), "数据库中至少应存在一个可用版块");
        return boards.get(0);
    }

    @Test
    // 测试查询指定数量的首页版块。
    void selectByNum() {
        List<Board> boards = boardService.selectByNum(3);
        assertNotNull(boards, "版块列表不应为 null");
        assertTrue(boards.size() <= 3, "返回数量不应超过请求数量");
        boards.forEach(b -> assertNotNull(b.getName(), "版块名称不应为空"));
    }

    @Test
    @Transactional
    // 测试版块帖子数加 1。
    void addOneArticleCountById() {
        Board board = anyBoard();
        int before = board.getArticleCount();

        boardService.addOneArticleCountById(board.getId());

        Board after = boardService.selectById(board.getId());
        assertEquals(before + 1, after.getArticleCount(), "版块帖子数应加 1");
    }

    @Test
    // 测试按 ID 查询版块。
    void selectById() {
        Board board = anyBoard();

        Board found = boardService.selectById(board.getId());

        assertNotNull(found, "按 ID 应能查到版块");
        assertEquals(board.getId(), found.getId());
        assertNotNull(found.getName(), "版块名称不应为空");
    }

    @Test
    @Transactional
    // 测试版块帖子数减 1。
    void subOneArticleCountById() {
        Board board = anyBoard();
        // 先加一次，保证计数大于 0，避免减到负数导致断言失真
        boardService.addOneArticleCountById(board.getId());
        int before = boardService.selectById(board.getId()).getArticleCount();

        boardService.subOneArticleCountById(board.getId());

        Board after = boardService.selectById(board.getId());
        assertEquals(before - 1, after.getArticleCount(), "版块帖子数应减 1");
    }
}
