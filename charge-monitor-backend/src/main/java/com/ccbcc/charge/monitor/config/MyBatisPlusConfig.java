package com.ccbcc.charge.monitor.config;

import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * MyBatis-Plus 配置类
 *
 * 主要作用：
 * 1. 注册 MyBatis-Plus 分页插件
 * 2. 支持 Page<T> 分页查询
 */
@Configuration
public class MyBatisPlusConfig {

    /**
     * MyBatis-Plus 核心插件
     */
    @Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor() {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();

        PaginationInnerInterceptor paginationInnerInterceptor =
                new PaginationInnerInterceptor(DbType.MYSQL);

        /*
         * 溢出总页数后是否回到第一页。
         * false 表示不自动回到第一页。
         */
        paginationInnerInterceptor.setOverflow(false);

        /*
         * 单页最大条数。
         * 防止一次查询过多数据。
         */
        paginationInnerInterceptor.setMaxLimit(500L);

        interceptor.addInnerInterceptor(paginationInnerInterceptor);

        return interceptor;
    }
}