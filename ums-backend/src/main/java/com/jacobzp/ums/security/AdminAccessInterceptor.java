package com.jacobzp.ums.security;

import com.jacobzp.ums.dto.ErrorResponse;
import com.jacobzp.ums.service.AdminUserService;
import com.jacobzp.ums.service.AdminUserService.Access;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import tools.jackson.databind.ObjectMapper;

/**
 * <p>
 * 描述：管理员鉴权拦截器
 * </p>
 * @author Jacobzp
 * @version 1.0.0
 */
@Component
public class AdminAccessInterceptor implements HandlerInterceptor {
    private final AdminUserService adminUserService;
    private final ObjectMapper objectMapper;

    public AdminAccessInterceptor(AdminUserService adminUserService, ObjectMapper objectMapper) {
        this.adminUserService = adminUserService;
        this.objectMapper = objectMapper;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws IOException {
        var session = request.getSession(false);
        Object sessionUserId = null;
        if (session != null) {
            try {
                sessionUserId = session.getAttribute(SessionAttributes.USER_ID);
            } catch (IllegalStateException ignored) {
                // 另一个请求可能已使该会话失效。
            }
        }
        Access access = sessionUserId instanceof Long userId
                ? adminUserService.checkAccess(userId)
                : Access.UNAUTHENTICATED;
        return switch (access) {
            case ALLOWED -> true;
            case UNAUTHENTICATED -> reject(response, HttpStatus.UNAUTHORIZED, "请先登录");
            case FORBIDDEN -> reject(response, HttpStatus.FORBIDDEN, "需要管理员权限");
        };
    }

    private boolean reject(HttpServletResponse response, HttpStatus status, String message) throws IOException {
        response.setStatus(status.value()); // 设置 HTTP 状态码
        response.setContentType(MediaType.APPLICATION_JSON_VALUE); // 告诉响应内容是 JSON
        objectMapper.writeValue(response.getOutputStream(), new ErrorResponse(message)); // 把错误对象转换为 JSON，写入 HTTP 响应体。
        return false;
    }
}
