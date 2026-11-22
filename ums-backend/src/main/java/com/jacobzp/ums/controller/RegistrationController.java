package com.jacobzp.ums.controller;

import com.jacobzp.ums.service.AccountAlreadyExistsException;
import com.jacobzp.ums.service.RegistrationService;
import com.jacobzp.ums.dto.ErrorResponse;
import com.jacobzp.ums.dto.RegistrationRequest;
import com.jacobzp.ums.dto.RegistrationResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * <p>
 * 描述：注册控制层
 * </p>
 * @author Jacobzp
 * @version 1.0.0
 */
@RestController
@Tag(name = "用户认证")
public class RegistrationController {
    private final RegistrationService registrationService;

    public RegistrationController(RegistrationService registrationService) {
        this.registrationService = registrationService;
    }

    @PostMapping("/api/user/register")
    @Operation(summary = "用户注册", description = "创建普通用户；账号不可重复，密码以 PBKDF2 哈希存储。")
    @ApiResponse(responseCode = "201", description = "注册成功", content = @Content(schema = @Schema(implementation = RegistrationResponse.class)))
    @ApiResponse(responseCode = "400", description = "账号或密码格式不合法", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "409", description = "账号已存在", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    public ResponseEntity<?> register(@RequestBody RegistrationRequest request) {
        // 前端传入的账号和密码校验
        if (request.getAccount() == null || !request.getAccount().matches("[A-Za-z0-9]{1,64}")
                || request.getPassword() == null || request.getPassword().codePointCount(0, request.getPassword().length()) < 8) {
            return ResponseEntity.badRequest().body(new ErrorResponse("账号或密码不符合要求"));
        }

        try {
            // 注册账号
            Long userId = registrationService.register(request);
            // 返回 HTTP 响应: 状态码是 201 Created，响应体包含新用户的 id。
            return ResponseEntity.status(HttpStatus.CREATED).body(new RegistrationResponse(userId));
        } catch (AccountAlreadyExistsException ex) {
            // 捕捉到 "账号已存在" 异常
            return ResponseEntity.status(HttpStatus.CONFLICT).body(new ErrorResponse("账号已存在"));
        }
    }
}
