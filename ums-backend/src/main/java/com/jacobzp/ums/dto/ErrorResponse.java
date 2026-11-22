package com.jacobzp.ums.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import io.swagger.v3.oas.annotations.media.Schema;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ErrorResponse {
    @Schema(description = "错误信息", example = "账号或密码不符合要求")
    private String message;
}
