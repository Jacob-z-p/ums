package com.jacobzp.ums.controller;

import com.jacobzp.ums.dto.AdminUserPage;
import com.jacobzp.ums.dto.AdminUserQuery;
import com.jacobzp.ums.dto.ErrorResponse;
import com.jacobzp.ums.service.AdminUserService;
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
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

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
    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE) // HTTP GET请求，声明接口提供 JSON 格式的响应(application/json)
    @Operation(summary = "分页查询用户", description = "查询未删除用户，按 ID 降序，支持按完整登录账号筛选。") // 接口文档的接口描述
    @ApiResponse(responseCode = "200", description = "用户分页",
            content = @Content(schema = @Schema(implementation = AdminUserPage.class))) // 描述接口在200状态码下的响应。
    @ApiResponse(responseCode = "400", description = "筛选或分页参数不合法",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class))) // 描述接口在400状态码下的响应。
    @ApiResponse(responseCode = "401", description = "没有有效会话或账号不可用",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class))) // 描述接口在401状态码下的响应。
    @ApiResponse(responseCode = "403", description = "需要管理员权限",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class))) // 描述接口在403状态码下的响应。
    public ResponseEntity<?> list(@ParameterObject @ModelAttribute AdminUserQuery query, BindingResult binding) {
        String account = query.getAccount();
        if (binding.hasErrors() || query.getPage() < 1 || query.getSize() < 1 || query.getSize() > 100
                || (account != null && !account.matches("[A-Za-z0-9]{1,64}"))) {
            return ResponseEntity.badRequest().body(new ErrorResponse("筛选或分页参数不合法"));
        }
        return ResponseEntity.ok(adminUserService.list(query));
    }


    /**
     * 删除用户
     *
     * @param id 用户 ID
     * @return ResponseEntity
     */
    @DeleteMapping("/{id}")
    @Operation(summary = "删除普通用户", description = "逻辑删除普通用户。已删除账号仍保留注册占用。")
    @ApiResponse(responseCode = "204", description = "删除成功", content = @Content)
    @ApiResponse(responseCode = "400", description = "用户 ID 不合法",
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "401", description = "没有有效会话或账号不可用",
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "403", description = "需要管理员权限，或目标是管理员",
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "404", description = "用户不存在或已删除",
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorResponse.class)))
    public ResponseEntity<?> delete(@PathVariable long id) {
        if (id <= 0) {
            return invalidUserId();
        }
        return switch (adminUserService.delete(id)) {
            case DELETED -> ResponseEntity.noContent().build();
            case NOT_FOUND -> ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ErrorResponse("用户不存在"));
            case ADMIN_PROTECTED -> ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(new ErrorResponse("不能删除管理员账号"));
        };
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    private ResponseEntity<ErrorResponse> invalidUserId() {
        return ResponseEntity.badRequest().body(new ErrorResponse("用户 ID 不合法"));
    }
}
