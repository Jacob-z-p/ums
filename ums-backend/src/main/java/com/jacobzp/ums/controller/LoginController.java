package com.jacobzp.ums.controller;

import com.jacobzp.ums.model.Result;
import com.jacobzp.ums.model.ResultCode;
import com.jacobzp.ums.model.dto.LoginDTO;
import com.jacobzp.ums.model.vo.LoginVO;
import com.jacobzp.ums.security.SessionAttributes;
import com.jacobzp.ums.service.LoginService;
import com.jacobzp.ums.validation.CredentialsValidator;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
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
    @ApiResponse(responseCode = "200", description = "登录成功，返回脱敏用户信息并设置 Session Cookie", useReturnTypeSchema = true)
    @ApiResponse(responseCode = "400", description = "1001 请求参数无效", content = @Content(schema = @Schema(implementation = Result.class)))
    @ApiResponse(responseCode = "401", description = "2001 账号或密码错误，或账号不可用",
            content = @Content(schema = @Schema(implementation = Result.class)))
    public ResponseEntity<Result<LoginVO>> login(@RequestBody LoginDTO request, HttpServletRequest servletRequest) {
        if (!CredentialsValidator.isValid(request.getAccount(), request.getPassword())) {
            return ResponseEntity.badRequest().body(Result.failure(ResultCode.INVALID_PARAMETER, CredentialsValidator.REQUIREMENTS));
        }
        var user = loginService.login(request);
        if (user.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Result.failure(ResultCode.INVALID_CREDENTIALS, "无法使用提供的凭据登录"));
        }
        SessionAttributes.invalidate(servletRequest);
        servletRequest.getSession(true).setAttribute(SessionAttributes.USER_ID, user.get().getId());
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(Result.success(user.get()));
    }

    @GetMapping("/api/user/current")
    @Operation(summary = "查询当前登录用户", security = @SecurityRequirement(name = "sessionCookie"))
    @ApiResponse(responseCode = "200", description = "返回脱敏用户信息", useReturnTypeSchema = true)
    @ApiResponse(responseCode = "401", description = "2003 没有有效会话或账号不可用",
            content = @Content(schema = @Schema(implementation = Result.class)))
    public ResponseEntity<Result<LoginVO>> current(HttpServletRequest request) {
        Long userId = SessionAttributes.userId(request);
        if (userId != null) {
            var user = loginService.findActiveUser(userId);
            if (user.isPresent()) {
                return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(Result.success(user.get()));
            }
            SessionAttributes.invalidate(request);
        }
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).cacheControl(CacheControl.noStore())
                .body(Result.failure(ResultCode.LOGIN_REQUIRED, "当前登录会话无效或账号不可用"));
    }

    @PostMapping("/api/user/logout")
    @Operation(summary = "退出登录", description = "使当前 Session 失效，允许重复调用。")
    @ApiResponse(responseCode = "200", description = "已退出，data 为 null", useReturnTypeSchema = true)
    public ResponseEntity<Result<Void>> logout(HttpServletRequest request) {
        SessionAttributes.invalidate(request);
        return ResponseEntity.ok(Result.success(null));
    }
}
