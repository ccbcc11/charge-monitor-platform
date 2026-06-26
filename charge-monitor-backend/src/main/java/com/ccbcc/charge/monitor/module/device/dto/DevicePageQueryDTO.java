package com.ccbcc.charge.monitor.module.device.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 设备分页查询参数
 */
@Data
@Schema(description = "设备分页查询参数")
public class DevicePageQueryDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 页码
     */
    @Min(value = 1, message = "页码不能小于1")
    @Schema(description = "页码，默认1", example = "1")
    private Long pageNo = 1L;

    /**
     * 每页数量
     */
    @Min(value = 1, message = "每页数量不能小于1")
    @Max(value = 100, message = "每页数量不能超过100")
    @Schema(description = "每页数量，默认10，最大100", example = "10")
    private Long pageSize = 10L;

    /**
     * 设备编号，模糊查询
     */
    @Schema(description = "设备编号，模糊查询", example = "CP-0001")
    private String deviceCode;

    /**
     * 设备名称，模糊查询
     */
    @Schema(description = "设备名称，模糊查询", example = "快充")
    private String deviceName;

    /**
     * 所属站点，模糊查询
     */
    @Schema(description = "所属站点，模糊查询", example = "城东")
    private String stationName;

    /**
     * 所属区域
     */
    @Schema(description = "所属区域", example = "城东区")
    private String region;

    /**
     * 设备类型：AC交流桩，DC直流桩
     */
    @Pattern(regexp = "AC|DC", message = "设备类型只能为AC或DC")
    @Schema(description = "设备类型：AC交流桩，DC直流桩", example = "DC")
    private String deviceType;

    /**
     * 在线状态：0离线，1在线
     */
    @Min(value = 0, message = "在线状态只能为0或1")
    @Max(value = 1, message = "在线状态只能为0或1")
    @Schema(description = "在线状态：0离线，1在线", example = "1")
    private Integer onlineStatus;

    /**
     * 运行状态：0停用，1正常，2异常
     */
    @Min(value = 0, message = "运行状态只能为0、1、2")
    @Max(value = 2, message = "运行状态只能为0、1、2")
    @Schema(description = "运行状态：0停用，1正常，2异常", example = "1")
    private Integer runningStatus;
}