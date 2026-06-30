package com.ccbcc.charge.monitor.module.alarm.websocket;

import java.util.List;

/**
 * 告警推送服务
 */
public interface AlarmPushService {

    /**
     * 根据告警 ID 列表推送告警消息
     */
    void pushAlarmMessages(List<Long> alarmIds);
}
