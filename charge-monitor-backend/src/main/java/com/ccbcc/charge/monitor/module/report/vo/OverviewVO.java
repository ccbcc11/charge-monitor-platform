package com.ccbcc.charge.monitor.module.report.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 今日运行概览返回
 */
@Data
@Accessors(chain = true)
@Schema(description = "今日运行概览返回")
public class OverviewVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 设备总数
     */
    @Schema(description = "设备总数", example = "100")
    private Long deviceTotal;

    /**
     * 在线设备数
     */
    @Schema(description = "在线设备数", example = "87")
    private Long onlineCount;

    /**
     * 离线设备数
     */
    @Schema(description = "离线设备数", example = "13")
    private Long offlineCount;

    /**
     * 当前异常设备数
     */
    @Schema(description = "当前异常设备数", example = "5")
    private Long abnormalDeviceCount;

    /**
     * 今日告警数
     */
    @Schema(description = "今日告警数", example = "25")
    private Long todayAlarmCount;

    /**
     * 今日严重告警数
     */
    @Schema(description = "今日严重告警数", example = "3")
    private Long seriousAlarmCount;

    /**
     * 未处理告警数
     *
     * MVP 阶段定义：
     * alarm_status in (0, 1)
     * 即未确认和已确认但未恢复的告警。
     */
    @Schema(description = "未处理告警数", example = "10")
    private Long unhandledAlarmCount;

    /**
     * 在线率，单位 %
     */
    @Schema(description = "在线率，单位%", example = "87.00")
    private BigDecimal onlineRate;
}