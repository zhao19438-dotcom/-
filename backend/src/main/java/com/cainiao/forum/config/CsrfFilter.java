package com.cainiao.forum.config;

import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * CSRF 防护过滤器：
 * 项目使用 Session Cookie 认证，浏览器会在同域请求里自动带上 Cookie，
 * 恶意第三方站点可以借用户的登录态伪造 POST 请求。
 * 防护方式：所有写请求（POST/PUT/DELETE）必须携带自定义头 X-Requested-With，
 * 该头受同源策略保护，跨站表单无法伪造；前端 axios 已全局携带。
 */
@Component
public class CsrfFilter extends OncePerRequestFilter {

    // 前端统一携带的请求头。
    private static final String REQUIRED_HEADER = "X-Requested-With";

    private static final String REQUIRED_VALUE = "XMLHttpRequest";

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        String method = request.getMethod();

        // 只校验会修改数据的请求，GET/HEAD/OPTIONS 直接放行。
        if ("GET".equalsIgnoreCase(method) || "HEAD".equalsIgnoreCase(method) || "OPTIONS".equalsIgnoreCase(method)) {
            filterChain.doFilter(request, response);
            return;
        }

        if (!REQUIRED_VALUE.equals(request.getHeader(REQUIRED_HEADER))) {
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"code\":1003,\"message\":\"非法请求，缺少防伪请求头\",\"data\":null}");
            return;
        }

        filterChain.doFilter(request, response);
    }
}
