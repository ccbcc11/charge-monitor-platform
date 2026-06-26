package com.ccbcc.charge.monitor.module.device.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 新增设备请求参数
 */
@Data
@Schema(description = "新增设备请求参数")
public class DeviceCreateDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 设备编号，唯一
     */
    @NotBlank(message = "设备编号不能为空")
    @Size(max = 64, message = "设备编号长度不能超过64个字符")
    @Schema(description = "设备编号，唯一", example = "CP-0006", requiredMode = Schema.RequiredMode.REQUIRED)
    private String deviceCode;

    /**
     * 设备名称
     */
    @NotBlank(message = "设备名称不能为空")
    @Size(max = 100, message = "设备名称长度不能超过100个字符")
    @Schema(description = "设备名称", example = "六号直流快充桩", requiredMode = Schema.RequiredMode.REQUIRED)
    private String deviceName;

    /**
     * 所属站点
     */
    @Size(max = 100, message = "所属站点长度不能超过100个字符")
    @Schema(description = "所属站点", example = "城东充电站")
    private String stationName;

    /**
     * 所属区域
     */
    @Size(max = 50, message = "所属区域长度不能超过50个字符")
    @Schema(description = "所属区域", example = "城东区")
    private String region;

    /**
     * 设备类型：AC交流桩，DC直流桩
     */
    @NotBlank(message = "设备类型不能为空")
    @Pattern(regexp = "AC|DC", message = "设备类型只能为AC或DC")
    @Schema(description = "设备类型：AC交流桩，DC直流桩", example = "DC", requiredMode = Schema.RequiredMode.REQUIRED)
    private String deviceType;

    /**
     * 额定功率，单位kW
     */
    @DecimalMin(value = "0.00", inclusive = false, message = "额定功率必须大于0")
    @Schema(description = "额定功率，单位kW", example = "120.00")
    private BigDecimal ratedPower;

    /**
     * 经度
     */
    @DecimalMin(value = "-180.000000", message = "经度不能小于-180")
    @DecimalMax(value = "180.000000", message = "经度不能大于180")
    @Schema(description = "经度", example = "118.796900")
    private BigDecimal longitude;

    /**
     * 纬度
     */
    @DecimalMin(value = "-90.000000", message = "纬度不能小于-90")
    @DecimalMax(value = "90.000000", message = "纬度不能大于90")
    @Schema(description = "纬度", example = "32.060300")
    private BigDecimal latitude;

    /**
     * 运行状态：0停用，1正常，2异常
     */
    @Min(value = 0, message = "运行状态只能为0、1、2")
    @Max(value = 2, message = "运行状态只能为0、1、2")
    @Schema(description = "运行状态：0停用，1正常，2异常", example = "1")
    private Integer runningStatus;

    /**
     * 负责人用户ID
     */
    @Schema(description = "负责人用户ID", example = "2")
    private Long managerId;

    /**
     * 投运时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "投运时间", example = "2026-06-26 09:00:00")
    private LocalDateTime installTime;

    /**
     * 备注
     */
    @Size(max = 255, message = "备注长度不能超过255个字符")
    @Schema(description = "备注", example = "新增测试设备")
    private String remark;
}