package com.ccbcc.charge.monitor.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springdoc.core.customizers.OperationCustomizer;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Knife4j / OpenAPI 配置类
 */
@Configuration
public class Knife4jConfig {

    private static final String TOKEN_HEADER = "satoken";

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("新能源充电设施运行监测与智能告警平台接口文档")
                        .description("面向充电设施运维场景，提供设备管理、运行数据上报、实时状态监测、异常告警、工单流转和统计报表等接口。")
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("ccbcc")
                                .email("ccbcc@example.com")))
                .components(new Components()
                        .addSecuritySchemes(TOKEN_HEADER,
                                new SecurityScheme()
                                        .type(SecurityScheme.Type.APIKEY)
                                        .in(SecurityScheme.In.HEADER)
                                        .name(TOKEN_HEADER)
                                        .description("登录成功后返回的 token，请在请求头中携带：satoken: xxxxx")
                        )
                )
                .addSecurityItem(new SecurityRequirement().addList(TOKEN_HEADER));
    }

    /**
     * 关键：把 satoken 安全配置真正挂到每一个接口 Operation 上
     */
    private OperationCustomizer securityOperationCustomizer() {
        return (operation, handlerMethod) -> {
            operation.addSecurityItem(new SecurityRequirement().addList(TOKEN_HEADER));
            return operation;
        };
    }

    @Bean
    public GroupedOpenApi defaultApi() {
        return GroupedOpenApi.builder()
                .group("默认分组")
                .pathsToMatch("/api/**")
                .addOperationCustomizer(securityOperationCustomizer())
                .build();
    }

    @Bean
    public GroupedOpenApi authApi() {
        return GroupedOpenApi.builder()
                .group("认证模块")
                .pathsToMatch("/api/auth/**")
                .addOperationCustomizer(securityOperationCustomizer())
                .build();
    }

    @Bean
    public GroupedOpenApi deviceApi() {
        return GroupedOpenApi.builder()
                .group("设备模块")
                .pathsToMatch(
                        "/api/device/**",
                        "/api/device/data/**"
                )
                .addOperationCustomizer(securityOperationCustomizer())
                .build();
    }

    @Bean
    public GroupedOpenApi alarmApi() {
        return GroupedOpenApi.builder()
                .group("告警模块")
                .pathsToMatch("/api/alarm/**")
                .addOperationCustomizer(securityOperationCustomizer())
                .build();
    }

    @Bean
    public GroupedOpenApi reportApi() {
        return GroupedOpenApi.builder()
                .group("报表模块")
                .pathsToMatch("/api/report/**")
                .addOperationCustomizer(securityOperationCustomizer())
                .build();
    }
}