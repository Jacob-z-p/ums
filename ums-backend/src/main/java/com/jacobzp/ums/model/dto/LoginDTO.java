package com.jacobzp.ums.model.dto;

import lombok.Data;
import lombok.ToString;
import io.swagger.v3.oas.annotations.media.Schema;

@Data
public class LoginDTO {
    @Schema(description = "账号，仅允许 1 至 64 位英文字母和数字", example = "Alice123", requiredMode = Schema.RequiredMode.REQUIRED, pattern = "^[A-Za-z0-9]{1,64}$")
    private String account;
    @ToString.Exclude // 防止让 password 出现在自动生成的 toString() 结果中，导致泄露
    @Schema(description = "密码，至少 8 个字符", example = "password123", requiredMode = Schema.RequiredMode.REQUIRED, minLength = 8, format = "password")
    private String password;
}
