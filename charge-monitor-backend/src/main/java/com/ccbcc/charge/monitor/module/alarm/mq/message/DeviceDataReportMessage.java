package com.ccbcc.charge.monitor.module.alarm.mq.message;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 设备数据上报消息
 *
 * 当前只传关键 ID 和基础信息。
 * 消费者后续可以根据 deviceDataId 查询 device_data，
 * 再调用 AlarmDetectService 执行告警检测。
 */
@Data
@Accessors(chain = true)
public class DeviceDataReportMessage implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 设备运行数据ID
     */
    private Long deviceDataId;

    /**
     * 设备ID
     */
    private Long deviceId;

    /**
     * 设备编号
     */
    private String deviceCode;

    /**
     * 数据上报时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime reportTime;
}