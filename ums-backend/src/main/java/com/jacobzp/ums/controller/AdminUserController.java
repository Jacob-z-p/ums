package com.jacobzp.ums.controller;

import com.jacobzp.ums.model.Result;
import com.jacobzp.ums.model.ResultCode;
import com.jacobzp.ums.model.query.AdminUserQuery;
import com.jacobzp.ums.model.vo.AdminUserPageVO;
import com.jacobzp.ums.service.AdminUserService;
import com.jacobzp.ums.validation.CredentialsValidator;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * <p>
 * 描述：用户管理控制层
 * </p>
 * @author Jacobzp
 * @version 1.0.0
 */
@RestController
@RequestMapping("/api/admin/users")
@Tag(name = "管理员用户管理")
@SecurityRequirement(name = "sessionCookie")
public class AdminUserController {
    private final AdminUserService adminUserService;

    public AdminUserController(AdminUserService adminUserService) {
        this.adminUserService = adminUserService;
    }

    /**
     * 查询用户
     *
     * @param query 前端传入对象
     * @param binding 检查对象 query 接收请求参数有没有出错
     * @return ResponseEntity
     */
    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "分页查询用户", description = "查询未删除用户，按 ID 降序，支持按完整登录账号筛选。")
    @ApiResponse(responseCode = "200", description = "用户分页", useReturnTypeSchema = true)
    @ApiResponse(responseCode = "400", description = "1001 筛选或分页参数不合法",
            content = @Content(schema = @Schema(implementation = Result.class)))
    @ApiResponse(responseCode = "401", description = "2003 没有有效会话或账号不可用",
            content = @Content(schema = @Schema(implementation = Result.class)))
    @ApiResponse(responseCode = "403", description = "3001 需要管理员权限",
            content = @Content(schema = @Schema(implementation = Result.class)))
    public ResponseEntity<Result<AdminUserPageVO>> list(
            @ParameterObject @ModelAttribute AdminUserQuery query, BindingResult binding) {
        String account = query.getAccount();
        if (binding.hasErrors() || query.getPage() < 1 || query.getSize() < 1 || query.getSize() > 100
                || (account != null && !CredentialsValidator.isValidAccount(account))) {
            return ResponseEntity.badRequest().body(Result.failure(ResultCode.INVALID_PARAMETER,
                    "页码须为正整数，每页条数须为 1–100，登录账号须为 1–64 位英文字母或数字"));
        }
        return ResponseEntity.ok(Result.success(adminUserService.list(query)));
    }


    /**
     * 删除用户
     *
     * @param id 用户 ID
     * @return ResponseEntity
     */
    @DeleteMapping("/{id}")
    @Operation(summary = "删除普通用户", description = "逻辑删除普通用户。已删除账号仍保留注册占用。")
    @ApiResponse(responseCode = "200", description = "删除成功，data 为 null", useReturnTypeSchema = true)
    @ApiResponse(responseCode = "400", description = "1001 用户 ID 不合法",
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = Result.class)))
    @ApiResponse(responseCode = "401", description = "2003 没有有效会话或账号不可用",
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = Result.class)))
    @ApiResponse(responseCode = "403", description = "3001 需要管理员权限，或 3002 目标是管理员",
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = Result.class)))
    @ApiResponse(responseCode = "404", description = "3003 用户不存在或已删除",
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = Result.class)))
    public ResponseEntity<Result<Void>> delete(@PathVariable long id) {
        if (id <= 0) {
            return ResponseEntity.badRequest().body(Result.failure(ResultCode.INVALID_PARAMETER, "用户 ID 须为正整数"));
        }
        return switch (adminUserService.delete(id)) {
            case DELETED -> ResponseEntity.ok(Result.success(null));
            case NOT_FOUND -> ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Result.failure(ResultCode.USER_NOT_FOUND, "目标用户不存在或已删除"));
            case ADMIN_PROTECTED -> ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Result.failure(ResultCode.ADMIN_PROTECTED, "仅支持删除普通用户"));
        };
    }

}
