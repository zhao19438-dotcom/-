package com.cainiao.forum.services.impl;

import com.cainiao.forum.dao.ArticleMapper;
import com.cainiao.forum.dao.ArticleLikeMapper;
import com.cainiao.forum.dao.ArticleCollectMapper;
import com.cainiao.forum.exception.ApplicationException;
import com.cainiao.forum.model.Article;
import com.cainiao.forum.model.Board;
import com.cainiao.forum.model.User;
import com.cainiao.forum.services.IUserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ArticleAccessRegressionTest {
    @Mock ArticleMapper articleMapper;
    @Mock ArticleLikeMapper articleLikeMapper;
    @Mock ArticleCollectMapper articleCollectMapper;
    @Mock IUserService userService;
    @InjectMocks ArticleServiceImpl service;
    Article article;
    @BeforeEach void setup() {
        article = new Article();
        article.setId(10L); article.setUserId(1L); article.setState((byte) 1);
        article.setDeleteState((byte) 0); article.setVisitCount(0);
        Board board = new Board(); board.setId(3L); board.setDeleteState((byte) 0);
        article.setBoard(board);
        when(articleMapper.selectDetailById(10L)).thenReturn(article);
    }
    @Test void anonymousCannotReadHiddenArticle() {
        assertThrows(ApplicationException.class, () -> service.selectDetailById(10L, null));
        verify(articleMapper, never()).increaseVisitCount(anyLong());
    }
    @Test void otherStudentCannotReadHiddenArticle() {
        User student = new User(); student.setId(2L); student.setIsAdmin((byte) 0);
        when(userService.selectById(2L)).thenReturn(student);
        assertThrows(ApplicationException.class, () -> service.selectDetailById(10L, 2L));
    }
    @Test void authorCanReadHiddenArticle() { assertSame(article, service.selectDetailById(10L, 1L)); }
    @Test void deletedBoardContentIsNotPublic() {
        article.setState((byte) 0); article.getBoard().setDeleteState((byte) 1);
        assertThrows(ApplicationException.class, () -> service.selectDetailById(10L, null));
    }
}
