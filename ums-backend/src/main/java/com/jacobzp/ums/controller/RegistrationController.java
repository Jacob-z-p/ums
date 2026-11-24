package com.jacobzp.ums.controller;

import com.jacobzp.ums.model.Result;
import com.jacobzp.ums.model.ResultCode;
import com.jacobzp.ums.model.dto.RegistrationDTO;
import com.jacobzp.ums.model.vo.RegistrationVO;
import com.jacobzp.ums.service.RegistrationService;
import com.jacobzp.ums.validation.CredentialsValidator;
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
    @ApiResponse(responseCode = "201", description = "注册成功", useReturnTypeSchema = true)
    @ApiResponse(responseCode = "400", description = "1001 请求参数无效", content = @Content(schema = @Schema(implementation = Result.class)))
    @ApiResponse(responseCode = "409", description = "2002 账号已存在", content = @Content(schema = @Schema(implementation = Result.class)))
    public ResponseEntity<Result<RegistrationVO>> register(@RequestBody RegistrationDTO request) {
        if (!CredentialsValidator.isValid(request.getAccount(), request.getPassword())) {
            return ResponseEntity.badRequest().body(Result.failure(ResultCode.INVALID_PARAMETER, CredentialsValidator.REQUIREMENTS));
        }
        Long userId = registrationService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(Result.success(new RegistrationVO(userId)));
    }
}
