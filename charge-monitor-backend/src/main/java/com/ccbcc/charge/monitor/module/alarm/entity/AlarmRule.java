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
 * 告警规则表实体类
 */
@Data
@Accessors(chain = true)
@TableName("alarm_rule")
public class AlarmRule implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 规则编码
     */
    private String ruleCode;

    /**
     * 规则名称
     */
    private String ruleName;

    /**
     * 告警类型：
     * THRESHOLD 阈值
     * OFFLINE 离线
     * CONTINUOUS 连续异常
     * FLUCTUATION 波动异常
     */
    private String alarmType;

    /**
     * 适用设备类型：
     * NULL 表示全部设备
     * AC 表示交流充电桩
     * DC 表示直流充电桩
     */
    private String deviceType;

    /**
     * 指标名称：
     * temperature / voltage / current / network_delay / power / heartbeat
     */
    private String metricName;

    /**
     * 运算符：
     * >、>=、<、<=、=、!=
     */
    private String operator;

    /**
     * 阈值
     */
    private BigDecimal thresholdValue;

    /**
     * 窗口大小，例如最近 N 次
     *
     * 当前 THRESHOLD 阶段暂时只使用 1。
     */
    private Integer windowSize;

    /**
     * 触发次数，例如连续 N 次异常
     *
     * 当前 THRESHOLD 阶段暂时只使用 1。
     */
    private Integer triggerCount;

    /**
     * 告警等级：
     * 1 一般
     * 2 重要
     * 3 严重
     */
    private Integer alarmLevel;

    /**
     * 是否启用：
     * 0 禁用
     * 1 启用
     */
    private Integer enabled;

    /**
     * 备注
     */
    private String remark;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    private LocalDateTime updateTime;

    /**
     * 逻辑删除：
     * 0 未删除
     * 1 已删除
     */
    @TableLogic
    private Integer deleted;
}