package com.ccbcc.charge.monitor;

import org.apache.ibatis.annotations.Mapper;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 新能源充电设施运行监测与智能告警平台启动类
 */
@EnableScheduling
@SpringBootApplication
@MapperScan(
        basePackages = "com.ccbcc.charge.monitor.module",
        annotationClass = Mapper.class
)
public class ChargeMonitorApplication {

    public static void main(String[] args) {
        SpringApplication.run(ChargeMonitorApplication.class, args);
    }
}