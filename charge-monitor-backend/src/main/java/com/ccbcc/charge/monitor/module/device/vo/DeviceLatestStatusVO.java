package com.ccbcc.charge.monitor.module.device.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 设备最新状态返回
 */
@Data
@Accessors(chain = true)
@Schema(description = "设备最新状态返回")
public class DeviceLatestStatusVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 设备编号
     */
    @Schema(description = "设备编号", example = "CP-0001")
    private String deviceCode;

    /**
     * 设备名称
     */
    @Schema(description = "设备名称", example = "一号直流快充桩")
    private String deviceName;

    /**
     * 在线状态：0离线，1在线
     */
    @Schema(description = "在线状态：0离线，1在线", example = "1")
    private Integer onlineStatus;

    /**
     * 运行状态：0停用，1正常，2异常
     */
    @Schema(description = "运行状态：0停用，1正常，2异常", example = "1")
    private Integer runningStatus;

    /**
     * 电压，单位V
     */
    @Schema(description = "电压，单位V", example = "221.50")
    private BigDecimal voltage;

    /**
     * 电流，单位A
     */
    @Schema(description = "电流，单位A", example = "32.40")
    private BigDecimal currentValue;

    /**
     * 功率，单位kW
     */
    @Schema(description = "功率，单位kW", example = "7.20")
    private BigDecimal power;

    /**
     * 温度，单位℃
     */
    @Schema(description = "温度，单位℃", example = "45.20")
    private BigDecimal temperature;

    /**
     * SOC，单位%
     */
    @Schema(description = "SOC，单位%", example = "68.50")
    private BigDecimal soc;

    /**
     * 网络延迟，单位ms
     */
    @Schema(description = "网络延迟，单位ms", example = "90")
    private Integer networkDelay;

    /**
     * 故障码
     */
    @Schema(description = "故障码", example = "NORMAL")
    private String faultCode;

    /**
     * 设备上报时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "设备上报时间", example = "2026-06-26 14:30:00")
    private LocalDateTime reportTime;

    /**
     * 是否来自 Redis 缓存
     */
    @Schema(description = "是否来自Redis缓存", example = "true")
    private Boolean fromCache;
}