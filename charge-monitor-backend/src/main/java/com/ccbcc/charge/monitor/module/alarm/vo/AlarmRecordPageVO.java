package com.ccbcc.charge.monitor.module.alarm.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 告警记录分页列表返回
 */
@Data
@Accessors(chain = true)
@Schema(description = "告警记录分页列表返回")
public class AlarmRecordPageVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 告警ID
     */
    @Schema(description = "告警ID", example = "2001")
    private Long id;

    /**
     * 告警编号
     */
    @Schema(description = "告警编号", example = "AL202606261430000001")
    private String alarmNo;

    /**
     * 设备ID
     */
    @Schema(description = "设备ID", example = "1")
    private Long deviceId;

    /**
     * 设备编号
     */
    @Schema(description = "设备编号", example = "CP-0001")
    private String deviceCode;

    /**
     * 告警类型：THRESHOLD/OFFLINE/CONTINUOUS/FLUCTUATION/COMBINATION
     */
    @Schema(description = "告警类型", example = "THRESHOLD")
    private String alarmType;

    /**
     * 告警指标：temperature/voltage/current/power/network_delay
     */
    @Schema(description = "告警指标", example = "temperature")
    private String alarmMetric;

    /**
     * 告警等级：1一般，2重要，3严重
     */
    @Schema(description = "告警等级：1一般，2重要，3严重", example = "3")
    private Integer alarmLevel;

    /**
     * 当前值
     */
    @Schema(description = "当前值", example = "85.20")
    private BigDecimal currentValue;

    /**
     * 阈值
     */
    @Schema(description = "阈值", example = "80.00")
    private BigDecimal thresholdValue;

    /**
     * 告警描述
     */
    @Schema(description = "告警描述", example = "设备温度超过阈值，当前温度85.20℃，阈值80.00℃")
    private String alarmMessage;

    /**
     * 告警状态：0未确认，1已确认，2已恢复
     */
    @Schema(description = "告警状态：0未确认，1已确认，2已恢复", example = "0")
    private Integer alarmStatus;

    /**
     * 首次发生时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "首次发生时间", example = "2026-06-26 14:30:00")
    private LocalDateTime firstTime;

    /**
     * 最近发生时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "最近发生时间", example = "2026-06-26 14:35:00")
    private LocalDateTime lastTime;

    /**
     * 告警发生次数
     */
    @Schema(description = "告警发生次数", example = "3")
    private Integer alarmCount;
}