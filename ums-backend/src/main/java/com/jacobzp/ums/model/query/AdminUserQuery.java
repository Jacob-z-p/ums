package com.jacobzp.ums.model.query;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
public class AdminUserQuery {
    @Schema(description = "完整登录账号（不区分大小写）", example = "Alice123",
            pattern = "^[A-Za-z0-9]{1,64}$")
    private String account;

    @Schema(description = "页码，从 1 开始", defaultValue = "1", minimum = "1")
    private int page = 1;

    @Schema(description = "每页条数", defaultValue = "20", minimum = "1", maximum = "100")
    private int size = 20;
}
