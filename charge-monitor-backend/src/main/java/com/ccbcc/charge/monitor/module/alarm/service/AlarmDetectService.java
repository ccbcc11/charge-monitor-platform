package com.ccbcc.charge.monitor.module.alarm.service;

import com.ccbcc.charge.monitor.module.device.entity.DeviceData;
import com.ccbcc.charge.monitor.module.device.entity.DeviceInfo;

import java.util.List;

/**
 * 告警检测服务
 *
 * 当前 MVP 阶段先实现基础阈值告警。
 * 后续可以在这里扩展：
 * 1. 动态 alarm_rule 规则
 * 2. 连续异常告警
 * 3. 波动异常告警
 * 4. RabbitMQ 消费端调用
 */
public interface AlarmDetectService {

    /**
     * 检测设备上报数据是否触发阈值告警
     *
     * @param deviceInfo 设备信息
     * @param deviceData 本次上报的运行数据
     * @return 本次触发或更新的告警 ID 列表
     */
    List<Long> detectThresholdAlarms(DeviceInfo deviceInfo, DeviceData deviceData);
}