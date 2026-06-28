package com.ccbcc.charge.monitor.module.alarm.service;

import com.ccbcc.charge.monitor.module.device.entity.DeviceData;
import com.ccbcc.charge.monitor.module.device.entity.DeviceInfo;

import java.util.List;

/**
 * 告警检测服务
 *
 * 当前已实现：
 * 1. THRESHOLD 阈值告警（单次数据判断）
 * 2. CONTINUOUS 连续异常告警（Redis 滑动窗口）
 *
 * 后续可扩展：
 * 3. FLUCTUATION 波动异常告警
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

    /**
     * 检测设备上报数据是否触发连续异常告警
     *
     * 基于 Redis 滑动窗口实现：
     * 1. 判断本次数据是否命中规则
     * 2. 将命中结果 push 到 Redis List
     * 3. 只保留最近 window_size 条
     * 4. 统计命中次数，达到 trigger_count 则触发告警
     *
     * @param deviceInfo 设备信息
     * @param deviceData 本次上报的运行数据
     * @return 本次触发或更新的告警 ID 列表
     */
    List<Long> detectContinuousAlarms(DeviceInfo deviceInfo, DeviceData deviceData);

    /**
     * 清除设备的连续异常滑动窗口
     *
     * 告警恢复后调用，避免旧窗口数据导致恢复后立即再次触发告警。
     *
     * @param deviceCode  设备编号
     * @param alarmMetric 告警指标（如 temperature）
     */
    void clearContinuousWindow(String deviceCode, String alarmMetric);
}