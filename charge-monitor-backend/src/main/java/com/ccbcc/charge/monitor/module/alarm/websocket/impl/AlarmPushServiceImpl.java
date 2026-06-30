package com.ccbcc.charge.monitor.module.alarm.websocket.impl;

import com.ccbcc.charge.monitor.module.alarm.entity.AlarmRecord;
import com.ccbcc.charge.monitor.module.alarm.mapper.AlarmRecordMapper;
import com.ccbcc.charge.monitor.module.alarm.websocket.AlarmPushService;
import com.ccbcc.charge.monitor.module.alarm.websocket.AlarmWebSocketHandler;
import com.ccbcc.charge.monitor.module.alarm.websocket.message.AlarmPushMessage;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 告警推送服务实现类
 *
 * 核心职责：
 * 1. 根据 alarmIds 查询 alarm_record
 * 2. 转成 AlarmPushMessage
 * 3. 通过 WebSocket 广播
 *
 * 注意：
 * WebSocket 推送失败不影响告警记录，仅记录日志。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AlarmPushServiceImpl implements AlarmPushService {

    private static final String EVENT_TYPE_ALARM_TRIGGERED = "ALARM_TRIGGERED";

    private final AlarmRecordMapper alarmRecordMapper;
    private final AlarmWebSocketHandler alarmWebSocketHandler;
    private final ObjectMapper objectMapper;

    @Override
    public void pushAlarmMessages(List<Long> alarmIds) {
        if (alarmIds == null || alarmIds.isEmpty()) {
            return;
        }

        List<AlarmRecord> alarmRecords = alarmRecordMapper.selectBatchIds(alarmIds);

        if (alarmRecords == null || alarmRecords.isEmpty()) {
            log.warn("告警推送失败，未查询到告警记录，alarmIds={}", alarmIds);
            return;
        }

        for (AlarmRecord alarmRecord : alarmRecords) {
            pushSingleAlarm(alarmRecord);
        }
    }

    private void pushSingleAlarm(AlarmRecord alarmRecord) {
        try {
            AlarmPushMessage pushMessage = convertToPushMessage(alarmRecord);

            String messageText = objectMapper.writeValueAsString(pushMessage);

            alarmWebSocketHandler.broadcast(messageText);

            log.info(
                    "告警 WebSocket 推送完成，alarmId={}，deviceCode={}，alarmType={}，alarmMetric={}",
                    alarmRecord.getId(),
                    alarmRecord.getDeviceCode(),
                    alarmRecord.getAlarmType(),
                    alarmRecord.getAlarmMetric()
            );
        } catch (Exception e) {
            log.warn(
                    "告警 WebSocket 推送失败，alarmId={}，deviceCode={}",
                    alarmRecord.getId(),
                    alarmRecord.getDeviceCode(),
                    e
            );
        }
    }

    private AlarmPushMessage convertToPushMessage(AlarmRecord alarmRecord) {
        return new AlarmPushMessage()
                .setEventType(EVENT_TYPE_ALARM_TRIGGERED)
                .setAlarmId(alarmRecord.getId())
                .setAlarmNo(alarmRecord.getAlarmNo())
                .setDeviceId(alarmRecord.getDeviceId())
                .setDeviceCode(alarmRecord.getDeviceCode())
                .setAlarmType(alarmRecord.getAlarmType())
                .setAlarmMetric(alarmRecord.getAlarmMetric())
                .setAlarmLevel(alarmRecord.getAlarmLevel())
                .setCurrentValue(alarmRecord.getCurrentValue())
                .setThresholdValue(alarmRecord.getThresholdValue())
                .setAlarmMessage(alarmRecord.getAlarmMessage())
                .setAlarmStatus(alarmRecord.getAlarmStatus())
                .setFirstTime(alarmRecord.getFirstTime())
                .setLastTime(alarmRecord.getLastTime())
                .setPushTime(LocalDateTime.now());
    }
}
