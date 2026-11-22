package com.jacobzp.ums.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class AdminUserResponse {
    private Long id;
    private String account;
    private String nickname;
    private String avatarUrl;
    @Schema(description = "0 普通用户，1 管理员")
    private Integer role;
    @Schema(description = "0 正常，1 禁用")
    private Integer status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
