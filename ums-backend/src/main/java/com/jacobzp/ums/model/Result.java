package com.jacobzp.ums.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@JsonInclude(JsonInclude.Include.ALWAYS)
public class Result<T> {
    @Schema(description = "业务码，0 表示成功", requiredMode = Schema.RequiredMode.REQUIRED)
    private int code;

    @Schema(description = "响应数据，失败或无数据时为 null",
            requiredMode = Schema.RequiredMode.REQUIRED, nullable = true)
    private T data;

    @Schema(description = "简短提示", requiredMode = Schema.RequiredMode.REQUIRED)
    private String message;

    @Schema(description = "具体信息，成功时为空字符串", requiredMode = Schema.RequiredMode.REQUIRED)
    private String description;

    public static <T> Result<T> success(T data) {
        return new Result<>(ResultCode.SUCCESS.getCode(), data, ResultCode.SUCCESS.getMessage(), "");
    }

    public static <T> Result<T> failure(ResultCode resultCode, String description) {
        return new Result<>(resultCode.getCode(), null, resultCode.getMessage(), description);
    }
}
