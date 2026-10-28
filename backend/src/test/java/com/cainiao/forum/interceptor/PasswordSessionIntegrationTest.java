package com.cainiao.forum.interceptor;

import com.cainiao.forum.model.User;
import com.cainiao.forum.services.IUserService;
import com.cainiao.forum.services.IAdminService;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.transaction.annotation.Transactional;
import javax.annotation.Resource;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class PasswordSessionIntegrationTest {
    @Resource MockMvc mvc;
    @Resource IUserService users;
    @Resource IAdminService admin;
    @Test void resetPasswordRevokesBothExistingSessions() throws Exception {
        String suffix = String.valueOf(System.nanoTime());
        User u = new User(); u.setUsername("s" + suffix.substring(suffix.length()-10)); u.setNickname("会话同学");
        u.setStudentNo(suffix); u.setRealName("会话测试"); u.setCollege("学院"); u.setClassName("班级"); u.setPassword("123456");
        users.createNormalUser(u);
        MockHttpSession first = login(u.getUsername()); MockHttpSession second = login(u.getUsername());
        admin.resetUserPassword(u.getId());
        mvc.perform(get("/user/info").session(first).header("X-Requested-With", "XMLHttpRequest")).andExpect(status().isUnauthorized());
        mvc.perform(get("/user/info").session(second).header("X-Requested-With", "XMLHttpRequest")).andExpect(status().isUnauthorized());
    }
    MockHttpSession login(String username) throws Exception {
        return (MockHttpSession) mvc.perform(post("/user/login").header("X-Requested-With", "XMLHttpRequest")
            .param("username", username).param("password", "123456"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.code").value(0))
            .andReturn().getRequest().getSession(false);
    }
}
