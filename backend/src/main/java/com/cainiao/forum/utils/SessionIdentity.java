package com.cainiao.forum.utils;

import com.cainiao.forum.config.AppConfig;
import com.cainiao.forum.model.User;
import com.cainiao.forum.services.IUserService;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

/** 公开接口的可选登录身份也必须验证最新凭据，不能信任过期 Session。 */
public final class SessionIdentity {
    private SessionIdentity() { }
    public static User current(HttpServletRequest request, IUserService users) {
        HttpSession session = request.getSession(false);
        if (session == null || !(session.getAttribute(AppConfig.USER_SESSION) instanceof User)) return null;
        User stored = (User) session.getAttribute(AppConfig.USER_SESSION);
        User current = stored.getId() == null ? null : users.selectById(stored.getId());
        Object credential = session.getAttribute(AppConfig.USER_CREDENTIAL);
        if (current == null || credential == null || !credential.equals(current.getPassword())
                || Byte.valueOf((byte)1).equals(current.getDeleteState())) {
            session.invalidate(); return null;
        }
        return current;
    }
}
