package com.cainiao.forum.services.impl;

import com.cainiao.forum.dao.*;
import com.cainiao.forum.model.*;
import com.cainiao.forum.services.*;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import javax.annotation.Resource;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
class BoardEditingIntegrationTest {
    @Resource IArticleService articles;
    @Resource IBoardService boards;
    @Resource IAdminService admin;
    @Resource IUserService users;
    @Resource BoardMapper boardMapper;
    @Resource ArticleMapper articleMapper;
    @Resource LostFoundMapper lostMapper;
    Board board(String type) {
        Board b = new Board(); b.setName("回归版块" + System.nanoTime() % 1000000);
        b.setBizType(type); b.setSort(999); b.setState((byte)0); b.setDeleteState((byte)0);
        b.setArticleCount(0); b.setCreateTime(new Date()); b.setUpdateTime(new Date());
        boardMapper.insertSelective(b); return b;
    }
    User user() {
        String suffix = String.valueOf(System.nanoTime());
        User u = new User(); u.setUsername("r" + suffix.substring(suffix.length()-10)); u.setNickname("回归同学");
        u.setStudentNo(suffix); u.setRealName("回归用户"); u.setCollege("学院"); u.setClassName("班级"); u.setPassword("123456");
        users.createNormalUser(u); return u;
    }
    @Test void availableBoardsAreNotLimitedToNine() {
        List<Long> ids = new ArrayList<>();
        for (int i=0;i<12;i++) ids.add(board("forum").getId());
        List<Board> actual = boards.selectAvailable();
        assertTrue(ids.stream().allMatch(id -> actual.stream().anyMatch(b -> id.equals(b.getId()))));
    }
    @Test void fullEditPersistsExtensionAndMovesCounts() {
        User u = user(); Board from = board("forum"); Board to = board("lostfound");
        Article a = new Article(); a.setUserId(u.getId()); a.setBoardId(from.getId()); a.setTitle("回归讨论"); a.setContent("原正文");
        articles.create(a);
        articles.modify(a.getId(), "更新寻物", "更新正文", to.getId(), "{\"lostType\":\"lost\",\"contact\":\"站内私信\",\"location\":\"图书馆\"}");
        assertEquals(to.getId(), articleMapper.selectByPrimaryKey(a.getId()).getBoardId());
        assertEquals("图书馆", lostMapper.selectByArticleId(a.getId()).getLocation());
        assertEquals(0, boardMapper.selectByPrimaryKey(from.getId()).getArticleCount());
        assertEquals(1, boardMapper.selectByPrimaryKey(to.getId()).getArticleCount());
        articles.modify(a.getId(), "返回普通讨论", "正文", from.getId(), "");
        assertNull(lostMapper.selectByArticleId(a.getId()));
        assertEquals(1, boardMapper.selectByPrimaryKey(from.getId()).getArticleCount());
    }
    @Test void deletedBoardDisappearsFromPublicLists() {
        User u = user(); Board b = board("forum");
        Article a = new Article(); a.setUserId(u.getId()); a.setBoardId(b.getId()); a.setTitle("待删除版块帖子"); a.setContent("正文"); articles.create(a);
        articles.collect(a.getId(), u.getId());
        admin.updateBoardDeleteState(b.getId(), (byte)1);
        assertEquals(0, articles.selectPage(b.getId(), null, "latest", 1, 10).getTotal());
        assertTrue(articles.selectCollectPage(u.getId(), 1, 10).getRecords().isEmpty());
        assertThrows(RuntimeException.class, () -> articles.selectDetailById(a.getId(), null));
    }
}
