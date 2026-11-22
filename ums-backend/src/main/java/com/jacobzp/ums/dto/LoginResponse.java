package com.jacobzp.ums.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import io.swagger.v3.oas.annotations.media.Schema;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class LoginResponse {
    @Schema(description = "用户 ID", example = "1")
    private Long id;
    @Schema(description = "账号", example = "Alice123")
    private String account;
    @Schema(description = "昵称", example = "Alice123")
    private String nickname;
    @Schema(description = "头像地址，可为空", nullable = true)
    private String avatarUrl;
    @Schema(description = "角色：0 普通用户，1 管理员", example = "0")
    private Integer role;
}
