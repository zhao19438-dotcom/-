package com.cainiao.forum.interceptor;

import com.cainiao.forum.config.AppConfig;
import com.cainiao.forum.model.User;
import com.cainiao.forum.services.IUserService;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.util.ReflectionTestUtils;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class SessionCredentialRegressionTest {
    @Test void passwordChangeInvalidatesOldSession() throws Exception {
        LoginInterceptor interceptor = new LoginInterceptor();
        IUserService users = mock(IUserService.class);
        ReflectionTestUtils.setField(interceptor, "userService", users);
        User old = user("old-hash"); User current = user("new-hash");
        when(users.selectById(1L)).thenReturn(current);
        MockHttpSession session = new MockHttpSession();
        session.setAttribute(AppConfig.USER_SESSION, old);
        session.setAttribute("USER_CREDENTIAL", "old-hash");
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/user/info");
        request.setSession(session); request.addHeader("X-Requested-With", "XMLHttpRequest");
        MockHttpServletResponse response = new MockHttpServletResponse();
        assertFalse(interceptor.preHandle(request, response, new Object()));
        assertEquals(401, response.getStatus());
        assertTrue(session.isInvalid());
    }
    private User user(String password) {
        User user = new User(); user.setId(1L); user.setPassword(password);
        user.setDeleteState((byte) 0); user.setIsAdmin((byte) 0); return user;
    }
}
