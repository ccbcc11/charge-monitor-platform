package com.ccbcc.charge.monitor.module.alarm.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 告警记录表实体类
 */
@Data
@Accessors(chain = true)
@TableName("alarm_record")
public class AlarmRecord implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 告警编号
     */
    private String alarmNo;

    /**
     * 设备ID
     */
    private Long deviceId;

    /**
     * 设备编号
     */
    private String deviceCode;

    /**
     * 告警类型：THRESHOLD/OFFLINE/CONTINUOUS/FLUCTUATION
     */
    private String alarmType;

    /**
     * 告警指标：temperature/voltage/current/network_delay/power
     */
    private String alarmMetric;

    /**
     * 告警等级：1一般，2重要，3严重
     */
    private Integer alarmLevel;

    /**
     * 当前值
     */
    private BigDecimal currentValue;

    /**
     * 阈值
     */
    private BigDecimal thresholdValue;

    /**
     * 告警描述
     */
    private String alarmMessage;

    /**
     * 告警状态：0未确认，1已确认，2已恢复，3已关闭
     */
    private Integer alarmStatus;

    /**
     * 首次发生时间
     */
    private LocalDateTime firstTime;

    /**
     * 最近发生时间
     */
    private LocalDateTime lastTime;

    /**
     * 恢复时间
     */
    private LocalDateTime recoverTime;

    /**
     * 发生次数
     */
    private Integer alarmCount;

    /**
     * 是否已生成工单：0否，1是
     */
    private Integer workOrderGenerated;

    /**
     * 告警去重键：设备+指标+类型
     */
    private String dedupKey;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    private LocalDateTime updateTime;

    /**
     * 逻辑删除：0未删除，1已删除
     */
    @TableLogic
    private Integer deleted;
}