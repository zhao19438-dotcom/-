package com.cainiao.forum.interceptor;

import com.cainiao.forum.config.AppConfig;
import com.cainiao.forum.model.User;
import com.cainiao.forum.services.IUserService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;


@Component
// 登录拦截器：在 Controller 执行前判断当前请求是否已经登录。
public class LoginInterceptor implements HandlerInterceptor {

    @Value("${cainiao-forum.login.url}")
    // 未登录时跳转到的登录页面，来自 application.yml。
    private String defaultURL;

    @Resource
    private IUserService userService;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {

        // 不创建新 Session，只检查已有 Session 中是否有登录用户。
        HttpSession session = request.getSession(false);

        if (session != null && session.getAttribute(AppConfig.USER_SESSION) instanceof User) {
            User sessionUser = (User) session.getAttribute(AppConfig.USER_SESSION);
            if (sessionUser.getId() != null) {
                // 每次请求都重新查数据库，确保禁用、删除、权限变化能立即生效。
                User currentUser = userService.selectById(sessionUser.getId());
                String credential = (String) session.getAttribute(AppConfig.USER_CREDENTIAL);
                if (currentUser != null && credential != null
                        && credential.equals(currentUser.getPassword())
                        && (currentUser.getDeleteState() == null || currentUser.getDeleteState() == 0)) {
                    // 后台接口（/admin/**）要求管理员身份，普通登录用户直接返回 403。
                    if (request.getRequestURI().startsWith(request.getContextPath() + "/admin")
                            && (currentUser.getIsAdmin() == null || currentUser.getIsAdmin() != 1)) {
                        response.sendError(HttpServletResponse.SC_FORBIDDEN);
                        return false;
                    }
                    session.setAttribute(AppConfig.USER_SESSION, currentUser);
                    return true;
                }
            }

            session.invalidate();
        }

        // 前后端分离后：AJAX 请求未登录返回 401 JSON，由前端 axios 统一跳登录页；
        // 其余（直接访问页面）仍按原逻辑重定向到登录页。
        if (isAjaxRequest(request)) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"code\":1101,\"message\":\"请先登录\",\"data\":null}");
            return false;
        }

        // 配置里可能写成 sign-in.html，这里统一补成 /sign-in.html 形式。
        String loginUrl = defaultURL;
        if (!loginUrl.startsWith("/")) {
            loginUrl = "/" + loginUrl;
        }

        response.sendRedirect(loginUrl);

        return false;

    }

    // 判断是否为 AJAX 请求：axios/jQuery 都会带 X-Requested-With 头。
    private boolean isAjaxRequest(HttpServletRequest request) {
        return "XMLHttpRequest".equals(request.getHeader("X-Requested-With"))
                || (request.getHeader("Accept") != null
                    && request.getHeader("Accept").contains("application/json"));
    }
}

