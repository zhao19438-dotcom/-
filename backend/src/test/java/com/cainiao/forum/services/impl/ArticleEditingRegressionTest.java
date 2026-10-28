package com.cainiao.forum.services.impl;

import com.cainiao.forum.biz.BizHandlerRegistry;
import com.cainiao.forum.biz.impl.LostFoundBizHandler;
import com.cainiao.forum.dao.*;
import com.cainiao.forum.exception.ApplicationException;
import com.cainiao.forum.model.*;
import com.cainiao.forum.services.IBoardService;
import com.cainiao.forum.services.ISensitiveWordService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;
import java.util.Collections;
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

class ArticleEditingRegressionTest {
    ArticleServiceImpl service = new ArticleServiceImpl();
    ArticleMapper articles = mock(ArticleMapper.class);
    LostFoundMapper lost = mock(LostFoundMapper.class);
    IBoardService boards = mock(IBoardService.class);
    Board destination;
    @BeforeEach void setup() {
        ReflectionTestUtils.setField(service, "articleMapper", articles);
        ReflectionTestUtils.setField(service, "boardService", boards);
        ReflectionTestUtils.setField(service, "lostFoundMapper", lost);
        ISensitiveWordService words = mock(ISensitiveWordService.class);
        when(words.filter(anyString())).thenAnswer(i -> i.getArgument(0));
        ReflectionTestUtils.setField(service, "sensitiveWordService", words);
        LostFoundBizHandler handler = new LostFoundBizHandler();
        ReflectionTestUtils.setField(handler, "lostFoundMapper", lost);
        ReflectionTestUtils.setField(service, "bizHandlerRegistry", new BizHandlerRegistry(Collections.singletonList(handler)));
        Article article = new Article(); article.setId(10L); article.setBoardId(3L); article.setDeleteState((byte)0);
        when(articles.selectByPrimaryKey(10L)).thenReturn(article);
        destination = new Board(); destination.setId(4L); destination.setBizType("lostfound"); destination.setState((byte)0); destination.setDeleteState((byte)0);
        when(boards.selectById(4L)).thenReturn(destination);
    }
    @Test void movingAndEditingLostFoundUpdatesAllRecords() {
        when(articles.updateByPrimaryKeySelective(any())).thenReturn(1);
        LostFound existing = new LostFound(); existing.setArticleId(10L); existing.setStatus((byte)1);
        when(lost.selectByArticleId(10L)).thenReturn(existing);
        service.modify(10L, "新的寻物信息", "修改后的正文", 4L, "{\"lostType\":\"lost\",\"contact\":\"新联系方式\",\"location\":\"新地点\"}");
        ArgumentCaptor<Article> update = ArgumentCaptor.forClass(Article.class);
        verify(articles).updateByPrimaryKeySelective(update.capture());
        assertEquals(4L, update.getValue().getBoardId());
        assertTrue(update.getValue().getExtJson().contains("新地点"));
        verify(boards).subOneArticleCountById(3L); verify(boards).addOneArticleCountById(4L);
        ArgumentCaptor<LostFound> details = ArgumentCaptor.forClass(LostFound.class);
        verify(lost).updateDetails(details.capture());
        assertEquals("新地点", details.getValue().getLocation());
        assertEquals((byte)1, details.getValue().getStatus());
    }
    @Test void invalidLostFoundIsRejectedBeforeWriting() {
        assertThrows(ApplicationException.class, () -> service.modify(10L, "寻物", "正文", 4L, "{}"));
        verify(articles, never()).updateByPrimaryKeySelective(any());
    }
    @Test void movingToForumRemovesLostFoundRecord() {
        destination.setBizType("forum");
        when(articles.updateByPrimaryKeySelective(any())).thenReturn(1);
        service.modify(10L, "普通讨论", "正文", 4L, "{}");
        verify(lost).deleteByArticleId(10L);
    }
}
