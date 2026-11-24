package com.jacobzp.ums.controller;

import com.jacobzp.ums.service.LoginService;
import com.jacobzp.ums.dto.ErrorResponse;
import com.jacobzp.ums.dto.LoginRequest;
import com.jacobzp.ums.dto.LoginResponse;
import com.jacobzp.ums.security.SessionAttributes;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Tag(name = "用户认证")
public class LoginController {
    private final LoginService loginService;

    public LoginController(LoginService loginService) {
        this.loginService = loginService;
    }

    @PostMapping("/api/user/login")
    @Operation(summary = "用户登录", description = "验证账号和密码，创建服务器端 Session 并通过 Cookie 返回会话标识。")
    @ApiResponse(responseCode = "200", description = "登录成功，返回脱敏用户信息并设置 Session Cookie", content = @Content(schema = @Schema(implementation = LoginResponse.class)))
    @ApiResponse(responseCode = "400", description = "账号或密码格式不合法", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "401", description = "账号或密码错误，或账号不可用", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    public ResponseEntity<?> login(@RequestBody LoginRequest request, HttpServletRequest servletRequest) {
        if (request.getAccount() == null || !request.getAccount().matches("[A-Za-z0-9]{1,64}")
                || request.getPassword() == null || request.getPassword().codePointCount(0, request.getPassword().length()) < 8) {
            return ResponseEntity.badRequest().body(new ErrorResponse("账号或密码不符合要求"));
        }
        var user = loginService.login(request); // var: 自动类型推导(jdk10及以上)
        if (user.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(new ErrorResponse("账号或密码错误"));
        }
        SessionAttributes.invalidate(servletRequest);
        servletRequest.getSession(true).setAttribute(SessionAttributes.USER_ID, user.get().getId());
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(user.get());
    }

    @GetMapping("/api/user/current")
    @Operation(summary = "查询当前登录用户", security = @SecurityRequirement(name = "sessionCookie"))
    @ApiResponse(responseCode = "200", description = "返回脱敏用户信息",
            content = @Content(schema = @Schema(implementation = LoginResponse.class)))
    @ApiResponse(responseCode = "401", description = "没有有效会话或账号不可用",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    public ResponseEntity<?> current(HttpServletRequest request) {
        Long userId = SessionAttributes.userId(request);
        if (userId != null) {
            var user = loginService.findActiveUser(userId);
            if (user.isPresent()) {
                return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(user.get());
            }
            SessionAttributes.invalidate(request);
        }
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).cacheControl(CacheControl.noStore())
                .body(new ErrorResponse("请先登录"));
    }

    @PostMapping("/api/user/logout")
    @Operation(summary = "退出登录", description = "使当前 Session 失效，允许重复调用。")
    @ApiResponse(responseCode = "204", description = "已退出", content = @Content)
    public ResponseEntity<Void> logout(HttpServletRequest request) {
        SessionAttributes.invalidate(request);
        return ResponseEntity.noContent().build();
    }
}
