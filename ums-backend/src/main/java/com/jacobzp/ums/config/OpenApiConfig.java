package com.jacobzp.ums.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {
    @Bean
    public OpenAPI userManagementOpenApi() {
        return new OpenAPI().info(new Info().title("用户管理系统 API")
                .description("用户注册、登录和管理员用户管理接口文档")
                .version("1.0.0")) // 设置接口文档基本信息
                .components(new Components().addSecuritySchemes("sessionCookie", new SecurityScheme()
                        .type(SecurityScheme.Type.APIKEY)
                        .in(SecurityScheme.In.COOKIE)
                        .name("JSESSIONID")
                        .description("通过登录接口获取会话 Cookie，管理接口要求管理员权限。"))); // 描述接口使用的 cookie 认证方式
    }
}
