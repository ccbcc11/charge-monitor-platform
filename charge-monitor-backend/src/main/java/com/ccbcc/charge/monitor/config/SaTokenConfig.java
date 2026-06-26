package com.ccbcc.charge.monitor.config;

import cn.dev33.satoken.interceptor.SaInterceptor;
import cn.dev33.satoken.router.SaRouter;
import cn.dev33.satoken.stp.StpUtil;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;


/**
 * Sa-Token 配置类
 *
 * 主要作用：
 * 1. 配置哪些接口需要登录
 * 2. 配置哪些接口可以放行
 * 3. 后续可扩展角色权限校验
 */
@Configuration
public class SaTokenConfig implements WebMvcConfigurer {

    @Override
    public void addInterceptors(InterceptorRegistry registry) {

        registry.addInterceptor(new SaInterceptor(handle -> {

                    /*
                     * 拦截 /api/** 下的接口。
                     * 排除登录接口、接口文档、静态资源等。
                     */
                    SaRouter
                            .match("/api/**")
                            .notMatch("/api/auth/login")
                            .notMatch("/api/auth/captcha")
                            .check(r -> StpUtil.checkLogin());

                }))
                .addPathPatterns("/**")

                /*
                 * Knife4j / Swagger / OpenAPI 相关路径放行。
                 * 否则未登录时接口文档打不开。
                 */
                .excludePathPatterns(
                        "/doc.html",
                        "/webjars/**",
                        "/favicon.ico",
                        "/swagger-ui.html",
                        "/swagger-ui/**",
                        "/v3/api-docs/**"
                );
    }
}