package com.cainiao.forum.config;

import org.springframework.core.Ordered;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * IP 限流过滤器：按「IP + 接口」做固定窗口计数，超过阈值直接拒绝。
 *
 * 说明：
 * 1. 部署在 Nginx 之后，真实 IP 从 X-Forwarded-For 取（Nginx 已配置该头）。
 * 2. 计数只存在内存里，重启即清零；单机部署够用，多机部署应换成 Redis。
 */
@Component
public class RateLimitFilter extends OncePerRequestFilter implements Ordered {

    // 统计窗口：60 秒。
    private static final long WINDOW_MILLIS = 60_000L;

    // 各类型接口的限流阈值（单位：次 / 60 秒）。
    private static final int LIMIT_LOGIN = 10;    // 登录、注册、找回密码
    private static final int LIMIT_WRITE = 20;    // 发帖、回复、举报
    private static final int LIMIT_CAPTCHA = 30;  // 验证码
    private static final int LIMIT_DEFAULT = 200; // 其他接口兜底

    // 单个计数桶：窗口起始时间 + 当前计数。
    private static class Bucket {
        long windowStart;
        int count;
    }

    private final ConcurrentHashMap<String, Bucket> buckets = new ConcurrentHashMap<>();

    @Override
    public int getOrder() {
        // 尽量早于其它过滤器执行，被限流的请求没必要再走后续校验。
        return Ordered.HIGHEST_PRECEDENCE + 10;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        String uri = request.getRequestURI();
        if (uri == null) {
            uri = "/";
        }
        String key = clientIp(request) + "|" + uri;
        int limit = limitFor(uri);

        long now = System.currentTimeMillis();
        long window = now / WINDOW_MILLIS;

        Bucket bucket = buckets.computeIfAbsent(key, k -> new Bucket());
        boolean overLimit;
        synchronized (bucket) {
            if (bucket.windowStart != window) {
                bucket.windowStart = window;
                bucket.count = 0;
            }
            bucket.count++;
            overLimit = bucket.count > limit;
        }

        // 顺带清理过期桶，避免长期运行导致 Map 无限增长。
        if (buckets.size() > 2000) {
            cleanExpired(window);
        }

        if (overLimit) {
            response.setStatus(429);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"code\":1006,\"message\":\"操作过于频繁，请稍后再试\",\"data\":null}");
            return;
        }

        filterChain.doFilter(request, response);
    }

    // 清理非当前窗口的计数桶。
    private void cleanExpired(long currentWindow) {
        Iterator<Map.Entry<String, Bucket>> it = buckets.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<String, Bucket> entry = it.next();
            Bucket bucket = entry.getValue();
            synchronized (bucket) {
                if (bucket.windowStart != currentWindow) {
                    it.remove();
                }
            }
        }
    }

    // 根据请求路径决定限流阈值。
    private int limitFor(String uri) {
        if (uri.contains("/user/login") || uri.contains("/user/register")
                || uri.contains("/user/resetPwd") || uri.contains("/user/securityQuestion")) {
            return LIMIT_LOGIN;
        }
        if (uri.contains("/user/captcha")) {
            return LIMIT_CAPTCHA;
        }
        if (uri.contains("/article/create") || uri.contains("/reply/create")
                || uri.contains("/report/create")) {
            return LIMIT_WRITE;
        }
        return LIMIT_DEFAULT;
    }

    // 取客户端真实 IP：优先 X-Forwarded-For（Nginx 反代场景），退回 remoteAddr。
    private String clientIp(HttpServletRequest request) {
        String remoteAddr = request.getRemoteAddr();
        // 只有来自本机反向代理的请求才信任转发头，直连后端时客户端无法伪造 IP 绕过限流。
        if ("127.0.0.1".equals(remoteAddr) || "0:0:0:0:0:0:0:1".equals(remoteAddr) || "::1".equals(remoteAddr)) {
            String xff = request.getHeader("X-Forwarded-For");
            if (xff != null && !xff.isEmpty()) {
                int idx = xff.indexOf(',');
                return idx > 0 ? xff.substring(0, idx).trim() : xff.trim();
            }
            String realIp = request.getHeader("X-Real-IP");
            if (realIp != null && !realIp.isEmpty()) {
                return realIp.trim();
            }
        }
        return remoteAddr;
    }
}
