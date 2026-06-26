package com.ccbcc.charge.monitor.module.alarm.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 告警记录分页查询参数
 */
@Data
@Schema(description = "告警记录分页查询参数")
public class AlarmRecordPageQueryDTO implements Serializable {

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
     * 设备编号
     */
    @Size(max = 64, message = "设备编号长度不能超过64个字符")
    @Schema(description = "设备编号", example = "CP-0001")
    private String deviceCode;

    /**
     * 告警类型：THRESHOLD/OFFLINE/CONTINUOUS/FLUCTUATION/COMBINATION
     */
    @Pattern(
            regexp = "THRESHOLD|OFFLINE|CONTINUOUS|FLUCTUATION|COMBINATION",
            message = "告警类型只能为THRESHOLD、OFFLINE、CONTINUOUS、FLUCTUATION或COMBINATION"
    )
    @Schema(description = "告警类型：THRESHOLD/OFFLINE/CONTINUOUS/FLUCTUATION/COMBINATION", example = "THRESHOLD")
    private String alarmType;

    /**
     * 告警指标：temperature/voltage/current/power/network_delay
     */
    @Pattern(
            regexp = "temperature|voltage|current|power|network_delay",
            message = "告警指标只能为temperature、voltage、current、power或network_delay"
    )
    @Schema(description = "告警指标：temperature/voltage/current/power/network_delay", example = "temperature")
    private String alarmMetric;

    /**
     * 告警等级：1一般，2重要，3严重
     */
    @Min(value = 1, message = "告警等级只能为1、2、3")
    @Max(value = 3, message = "告警等级只能为1、2、3")
    @Schema(description = "告警等级：1一般，2重要，3严重", example = "3")
    private Integer alarmLevel;

    /**
     * 告警状态：0未确认，1已确认，2已恢复
     */
    @Min(value = 0, message = "告警状态只能为0、1、2")
    @Max(value = 2, message = "告警状态只能为0、1、2")
    @Schema(description = "告警状态：0未确认，1已确认，2已恢复", example = "0")
    private Integer alarmStatus;

    /**
     * 开始时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "开始时间", example = "2026-06-26 00:00:00")
    private LocalDateTime startTime;

    /**
     * 结束时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "结束时间", example = "2026-06-26 23:59:59")
    private LocalDateTime endTime;
}