package com.jacobzp.ums.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {
    @Bean
    public OpenAPI userManagementOpenApi() {
        return new OpenAPI().info(new Info().title("用户管理系统 API")
                .description("用户注册和登录接口文档")
                .version("1.0.0"));
    }
}
