package com.ccbcc.charge.monitor.module.device.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 设备运行数据上报请求参数
 */
@Data
@Schema(description = "设备运行数据上报请求参数")
public class DeviceDataReportDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 设备编号
     */
    @NotBlank(message = "设备编号不能为空")
    @Size(max = 64, message = "设备编号长度不能超过64个字符")
    @Schema(description = "设备编号", example = "CP-0001", requiredMode = Schema.RequiredMode.REQUIRED)
    private String deviceCode;

    /**
     * 电压，单位V
     */
    @DecimalMin(value = "0.00", message = "电压不能小于0")
    @DecimalMax(value = "1000.00", message = "电压不能大于1000")
    @Schema(description = "电压，单位V", example = "221.50")
    private BigDecimal voltage;

    /**
     * 电流，单位A
     */
    @DecimalMin(value = "0.00", message = "电流不能小于0")
    @DecimalMax(value = "1000.00", message = "电流不能大于1000")
    @Schema(description = "电流，单位A", example = "32.40")
    private BigDecimal currentValue;

    /**
     * 功率，单位kW
     */
    @DecimalMin(value = "0.00", message = "功率不能小于0")
    @Schema(description = "功率，单位kW", example = "7.20")
    private BigDecimal power;

    /**
     * 温度，单位℃
     */
    @DecimalMin(value = "-40.00", message = "温度不能小于-40")
    @DecimalMax(value = "150.00", message = "温度不能大于150")
    @Schema(description = "温度，单位℃", example = "82.60")
    private BigDecimal temperature;

    /**
     * SOC，单位%
     */
    @DecimalMin(value = "0.00", message = "SOC不能小于0")
    @DecimalMax(value = "100.00", message = "SOC不能大于100")
    @Schema(description = "SOC，单位%", example = "68.50")
    private BigDecimal soc;

    /**
     * 网络延迟，单位ms
     */
    @Min(value = 0, message = "网络延迟不能小于0")
    @Schema(description = "网络延迟，单位ms", example = "90")
    private Integer networkDelay;

    /**
     * 故障码，NORMAL表示正常
     */
    @Size(max = 50, message = "故障码长度不能超过50个字符")
    @Schema(description = "故障码，NORMAL表示正常", example = "NORMAL")
    private String faultCode;

    /**
     * 设备上报时间
     */
    @NotNull(message = "设备上报时间不能为空")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "设备上报时间", example = "2026-06-26 14:30:00", requiredMode = Schema.RequiredMode.REQUIRED)
    private LocalDateTime reportTime;
}