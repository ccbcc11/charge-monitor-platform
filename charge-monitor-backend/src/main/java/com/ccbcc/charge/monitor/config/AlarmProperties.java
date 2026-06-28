package com.ccbcc.charge.monitor.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/**
 * MVP 阶段基础告警阈值配置
 *
 * 对应 application.yml：
 *
 * charge-monitor:
 *   alarm:
 *     temperature-threshold: 80.00
 *     voltage-low-threshold: 180.00
 *     network-delay-threshold: 200
 */
@Data
@Component
@ConfigurationProperties(prefix = "charge-monitor.alarm")
public class AlarmProperties {

    /**
     * 温度过高阈值
     */
    private BigDecimal temperatureThreshold = new BigDecimal("80.00");

    /**
     * 电压过低阈值
     */
    private BigDecimal voltageLowThreshold = new BigDecimal("180.00");

    /**
     * 网络延迟过高阈值，单位 ms
     */
    private Integer networkDelayThreshold = 200;
}