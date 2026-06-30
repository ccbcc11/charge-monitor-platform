package com.ccbcc.charge.monitor.module.alarm.websocket.message;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import lombok.experimental.Accessors;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 告警 WebSocket 推送消息
 */
@Data
@Accessors(chain = true)
public class AlarmPushMessage {

    /**
     * 事件类型：
     * ALARM_TRIGGERED 告警触发或更新
     */
    private String eventType;

    private Long alarmId;

    private String alarmNo;

    private Long deviceId;

    private String deviceCode;

    private String alarmType;

    private String alarmMetric;

    private Integer alarmLevel;

    private BigDecimal currentValue;

    private BigDecimal thresholdValue;

    private String alarmMessage;

    private Integer alarmStatus;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime firstTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime lastTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime pushTime;
}
