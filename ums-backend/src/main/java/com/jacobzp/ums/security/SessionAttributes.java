package com.jacobzp.ums.security;

import jakarta.servlet.http.HttpServletRequest;
import org.jspecify.annotations.Nullable;

public final class SessionAttributes {
    public static final String USER_ID = "userId";

    private SessionAttributes() {
    }

    /**
     * 获取 Session 中的 userId
     * @param request 请求
     * @return Long/null
     */
    public static @Nullable Long userId(HttpServletRequest request) {
        var session = request.getSession(false);
        if (session == null) {
            return null;
        }
        try {
            return session.getAttribute(USER_ID) instanceof Long userId ? userId : null;
        } catch (IllegalStateException ignored) {
            return null; // 另一个请求已使会话失效。
        }
    }

    public static void invalidate(HttpServletRequest request) {
        var session = request.getSession(false);
        if (session != null) {
            try {
                session.invalidate();
            } catch (IllegalStateException ignored) {
                // 另一个请求已使会话失效。
            }
        }
    }
}
