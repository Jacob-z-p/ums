package com.jacobzp.ums.model.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import io.swagger.v3.oas.annotations.media.Schema;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RegistrationVO {
    @Schema(description = "新用户 ID", example = "1")
    private Long id;
}
