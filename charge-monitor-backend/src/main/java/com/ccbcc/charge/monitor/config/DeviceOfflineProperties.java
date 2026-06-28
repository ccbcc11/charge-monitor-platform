package com.ccbcc.charge.monitor.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 设备离线检测配置
 *
 * 对应 application.yml：
 *
 * charge-monitor:
 *   device:
 *     offline:
 *       enabled: true
 *       fixed-delay: 30000
 *       offline-seconds: 60
 */
@Data
@Component
@ConfigurationProperties(prefix = "charge-monitor.device.offline")
public class DeviceOfflineProperties {

    /**
     * 是否开启设备离线检测
     */
    private Boolean enabled = true;

    /**
     * 定时任务执行间隔，单位：毫秒
     *
     * 注意：
     * 这里主要用于配置展示，真正的 @Scheduled 间隔在注解中通过配置项读取。
     */
    private Long fixedDelay = 30000L;

    /**
     * 设备超过多少秒未上报，认为离线
     */
    private Long offlineSeconds = 60L;

    public boolean isEnabled() {
        return Boolean.TRUE.equals(enabled);
    }
}