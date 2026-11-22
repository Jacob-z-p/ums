package com.jacobzp.ums.config;

import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * <p>
 * 描述：MyBatis-Plus 配置类
 * </p>
 * @author Jacobzp
 * @version 1.0.0
 */
@Configuration(proxyBeanMethods = false)
public class MybatisPlusConfig {
    @Bean // 将 mybatisPlusInterceptor 注入到 Spring 容器中，让 MyBatis-Plus 使用这个拦截器
    MybatisPlusInterceptor mybatisPlusInterceptor() {
        var pagination = new PaginationInnerInterceptor();
        pagination.setMaxLimit(100L);
        var interceptor = new MybatisPlusInterceptor();
        interceptor.addInnerInterceptor(pagination);
        return interceptor;
    }
}
