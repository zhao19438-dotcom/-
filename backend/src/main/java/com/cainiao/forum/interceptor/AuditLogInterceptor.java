package com.cainiao.forum.interceptor;

import com.cainiao.forum.config.AppConfig;
import com.cainiao.forum.model.User;
import com.cainiao.forum.services.IAuditLogService;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 审计日志拦截器：记录后台（/admin/**）的写操作。
 *
 * 说明：
 * 1. 采用拦截器而不是逐处在 Controller 里埋点，新增后台接口会自动被审计。
 * 2. 只记录 POST/PUT/PATCH/DELETE，查询请求不入库，避免日志被浏览操作淹没。
 * 3. 动作名按路径映射成中文业务说明，后台审计页面对管理员可读。
 * 4. 跳过 /admin/audit/ 自身，避免「查看日志」产生日志造成噪音。
 * 5. 记录参数时会对密码类字段脱敏，避免明文入库。
 */
@Component
public class AuditLogInterceptor implements HandlerInterceptor {

    @Resource
    private IAuditLogService auditLogService;

    // 需要脱敏的参数名（包含即打码）。
    private static final String[] MASK_KEYS = {"password", "newPassword", "oldPassword", "passwordRepeat", "securityAnswer"};

    /**
     * 后台写操作到可读动作的映射：key 为接口路径，value 为 {动作名, 目标类型}。
     * 未匹配到的接口回退为「请求方法 + 路径」，保证新增接口不会漏审计。
     */
    private static final Map<String, String[]> ACTION_RULES = new LinkedHashMap<>();

    static {
        ACTION_RULES.put("/admin/users/state", new String[]{"变更用户状态", "user"});
        ACTION_RULES.put("/admin/users/admin", new String[]{"变更管理员权限", "user"});
        ACTION_RULES.put("/admin/users/delete", new String[]{"删除用户", "user"});
        ACTION_RULES.put("/admin/users/resetPassword", new String[]{"重置用户密码", "user"});
        ACTION_RULES.put("/admin/boards/save", new String[]{"保存版块", "board"});
        ACTION_RULES.put("/admin/boards/state", new String[]{"变更版块状态", "board"});
        ACTION_RULES.put("/admin/boards/delete", new String[]{"删除版块", "board"});
        ACTION_RULES.put("/admin/articles/state", new String[]{"变更帖子状态", "article"});
        ACTION_RULES.put("/admin/articles/delete", new String[]{"删除帖子", "article"});
        ACTION_RULES.put("/admin/articles/top", new String[]{"设置帖子置顶", "article"});
        ACTION_RULES.put("/admin/articles/essence", new String[]{"设置帖子精华", "article"});
        ACTION_RULES.put("/admin/sensitive/add", new String[]{"新增敏感词", "sensitive"});
        ACTION_RULES.put("/admin/sensitive/delete", new String[]{"删除敏感词", "sensitive"});
        ACTION_RULES.put("/admin/sensitive/refresh", new String[]{"刷新敏感词缓存", "sensitive"});
        ACTION_RULES.put("/admin/report/handle", new String[]{"处理举报", "report"});
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response,
                                Object handler, Exception ex) {

        String method = request.getMethod();
        // 查询类请求不产生审计记录：管理员翻列表不应该刷屏。
        if (!"POST".equalsIgnoreCase(method)
                && !"PUT".equalsIgnoreCase(method)
                && !"PATCH".equalsIgnoreCase(method)
                && !"DELETE".equalsIgnoreCase(method)) {
            return;
        }

        String uri = request.getRequestURI();
        if (uri == null || uri.contains("/admin/audit/")) {
            return;
        }

        HttpSession session = request.getSession(false);
        User user = session == null ? null : (User) session.getAttribute(AppConfig.USER_SESSION);

        String[] rule = matchRule(uri);
        String action = rule != null ? rule[0] : method + " " + uri;
        String targetType = rule != null ? rule[1] : "admin";
        String targetId = resolveTargetId(request);
        String detail = buildDetail(request);

        auditLogService.log(user == null ? null : user.getId(),
                user == null ? null : user.getUsername(),
                action,
                targetType,
                targetId,
                detail,
                clientIp(request));
    }

    // 路径匹配：优先精确命中，未命中时按前缀匹配，兼容将来带子路径的接口。
    private String[] matchRule(String uri) {
        String[] exact = ACTION_RULES.get(uri);
        if (exact != null) {
            return exact;
        }
        for (Map.Entry<String, String[]> entry : ACTION_RULES.entrySet()) {
            if (uri.startsWith(entry.getKey())) {
                return entry.getValue();
            }
        }
        return null;
    }

    // 目标对象 id：不同接口的参数名不完全一致，按常见命名依次尝试。
    private String resolveTargetId(HttpServletRequest request) {
        String[] keys = {"id", "articleId", "userId", "boardId", "reportId"};
        for (String key : keys) {
            String value = request.getParameter(key);
            if (value != null && !value.isEmpty()) {
                return value;
            }
        }
        return null;
    }

    // 汇总请求参数，密码类字段统一替换为 ******。
    private String buildDetail(HttpServletRequest request) {
        Map<String, String[]> params = request.getParameterMap();
        if (params == null || params.isEmpty()) {
            return null;
        }
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, String[]> entry : params.entrySet()) {
            if (sb.length() > 0) {
                sb.append("&");
            }
            sb.append(entry.getKey()).append("=");
            String value = entry.getValue() != null && entry.getValue().length > 0 ? entry.getValue()[0] : "";
            sb.append(maskIfNeeded(entry.getKey(), value));
        }
        return sb.toString();
    }

    private String maskIfNeeded(String key, String value) {
        if (key == null) {
            return value;
        }
        String lower = key.toLowerCase();
        for (String mask : MASK_KEYS) {
            if (lower.contains(mask.toLowerCase())) {
                return "******";
            }
        }
        return value;
    }

    // 取客户端真实 IP：优先 X-Forwarded-For（Nginx 反代场景），退回 remoteAddr。
    private String clientIp(HttpServletRequest request) {
        String xff = request.getHeader("X-Forwarded-For");
        if (xff != null && !xff.isEmpty()) {
            int idx = xff.indexOf(',');
            return idx > 0 ? xff.substring(0, idx).trim() : xff.trim();
        }
        return request.getRemoteAddr();
    }
}
