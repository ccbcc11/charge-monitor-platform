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
 * 设备历史运行数据返回
 */
@Data
@Accessors(chain = true)
@Schema(description = "设备历史运行数据返回")
public class DeviceDataHistoryVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 数据ID
     */
    @Schema(description = "数据ID", example = "1001")
    private Long id;

    /**
     * 设备编号
     */
    @Schema(description = "设备编号", example = "CP-0001")
    private String deviceCode;

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
     * 入库时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "入库时间", example = "2026-06-26 14:30:01")
    private LocalDateTime createTime;
}