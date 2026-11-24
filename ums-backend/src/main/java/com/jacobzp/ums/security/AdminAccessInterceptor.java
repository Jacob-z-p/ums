package com.jacobzp.ums.security;

import com.jacobzp.ums.model.Result;
import com.jacobzp.ums.model.ResultCode;
import com.jacobzp.ums.service.AdminUserService;
import com.jacobzp.ums.service.AdminUserService.Access;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

import org.jspecify.annotations.NullMarked;
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
    @NullMarked // 明确该方法作用域内的类型默认非空
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws IOException {
        Long userId = SessionAttributes.userId(request);
        Access access = userId != null
                ? adminUserService.checkAccess(userId)
                : Access.UNAUTHENTICATED;
        return switch (access) {
            case ALLOWED -> true;
            case UNAUTHENTICATED -> reject(response, HttpStatus.UNAUTHORIZED,
                    Result.failure(ResultCode.LOGIN_REQUIRED, "当前登录会话无效或账号不可用"));
            case FORBIDDEN -> reject(response, HttpStatus.FORBIDDEN,
                    Result.failure(ResultCode.ADMIN_REQUIRED, "当前账号没有管理员权限"));
        };
    }

    private boolean reject(HttpServletResponse response, HttpStatus status, Result<Void> result) throws IOException {
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        objectMapper.writeValue(response.getOutputStream(), result);
        return false;
    }
}
