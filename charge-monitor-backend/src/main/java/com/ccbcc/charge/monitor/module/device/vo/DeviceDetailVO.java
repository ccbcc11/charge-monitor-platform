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
 * 设备详情返回
 */
@Data
@Accessors(chain = true)
@Schema(description = "设备详情返回")
public class DeviceDetailVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 设备ID
     */
    @Schema(description = "设备ID", example = "1")
    private Long id;

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
     * 所属站点
     */
    @Schema(description = "所属站点", example = "城东充电站")
    private String stationName;

    /**
     * 所属区域
     */
    @Schema(description = "所属区域", example = "城东区")
    private String region;

    /**
     * 设备类型：AC交流桩，DC直流桩
     */
    @Schema(description = "设备类型：AC交流桩，DC直流桩", example = "DC")
    private String deviceType;

    /**
     * 额定功率，单位kW
     */
    @Schema(description = "额定功率，单位kW", example = "120.00")
    private BigDecimal ratedPower;

    /**
     * 经度
     */
    @Schema(description = "经度", example = "118.796900")
    private BigDecimal longitude;

    /**
     * 纬度
     */
    @Schema(description = "纬度", example = "32.060300")
    private BigDecimal latitude;

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
     * 最近心跳时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "最近心跳时间", example = "2026-06-26 14:30:00")
    private LocalDateTime lastHeartbeat;

    /**
     * 负责人用户ID
     */
    @Schema(description = "负责人用户ID", example = "2")
    private Long managerId;

    /**
     * 投运时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "投运时间", example = "2025-01-10 09:00:00")
    private LocalDateTime installTime;

    /**
     * 备注
     */
    @Schema(description = "备注", example = "示例设备")
    private String remark;

    /**
     * 创建时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "创建时间", example = "2026-06-26 09:00:00")
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "更新时间", example = "2026-06-26 14:30:00")
    private LocalDateTime updateTime;
}