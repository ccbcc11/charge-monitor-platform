package com.ccbcc.charge.monitor.module.alarm.mq.consumer;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ccbcc.charge.monitor.common.constants.RabbitMqConstants;
import com.ccbcc.charge.monitor.module.alarm.mq.message.DeviceDataReportMessage;
import com.ccbcc.charge.monitor.module.alarm.service.AlarmDetectService;
import com.ccbcc.charge.monitor.module.alarm.websocket.AlarmPushService;
import com.ccbcc.charge.monitor.module.device.entity.DeviceData;
import com.ccbcc.charge.monitor.module.device.entity.DeviceInfo;
import com.ccbcc.charge.monitor.module.device.mapper.DeviceDataMapper;
import com.ccbcc.charge.monitor.module.device.mapper.DeviceInfoMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * 设备数据告警消费者
 *
 * 正式异步告警流程：
 *
 * 1. 消费设备数据上报 MQ 消息
 * 2. 根据 deviceDataId 查询 device_data
 * 3. 根据 deviceId 查询 device_info
 * 4. 调用 AlarmDetectService 执行动态规则告警检测
 * 5. 新增或更新 alarm_record
 * 6. 更新 Redis device:alarm:set
 *
 * 7. 事务提交成功后通过 WebSocket 推送告警消息
 *
 * 注意：
 * OFFLINE 离线告警仍由 DeviceOfflineCheckTask 负责。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DeviceDataAlarmConsumer {

    private final DeviceDataMapper deviceDataMapper;
    private final DeviceInfoMapper deviceInfoMapper;
    private final AlarmDetectService alarmDetectService;
    private final AlarmPushService alarmPushService;

    /**
     * 处理设备数据上报消息
     *
     * 使用事务的原因：
     * AlarmDetectService 内部会新增或更新 alarm_record。
     * 如果处理过程中发生异常，希望本次告警处理整体回滚。
     */
    @RabbitListener(queues = RabbitMqConstants.DEVICE_DATA_ALARM_QUEUE)
    @Transactional(rollbackFor = Exception.class)
    public void handleDeviceDataReportMessage(DeviceDataReportMessage message) {
        if (message == null) {
            log.warn("收到设备数据上报空 MQ 消息，跳过处理");
            return;
        }

        if (message.getDeviceDataId() == null) {
            log.warn("MQ 消息缺少 deviceDataId，跳过处理，message={}", message);
            return;
        }

        log.info(
                "开始异步处理设备告警消息，deviceDataId={}，deviceId={}，deviceCode={}，reportTime={}",
                message.getDeviceDataId(),
                message.getDeviceId(),
                message.getDeviceCode(),
                message.getReportTime()
        );

        /*
         * 1. 查询本次上报的设备运行数据
         */
        DeviceData deviceData = deviceDataMapper.selectById(message.getDeviceDataId());
        if (deviceData == null) {
            /*
             * 理论上不应该发生。
             * 因为 DeviceDataServiceImpl 是事务提交成功后才发送 MQ。
             * 如果这里查不到，大概率是消息脏数据或数据被删除。
             */
            log.warn(
                    "MQ 告警处理失败：未找到设备运行数据，deviceDataId={}，deviceCode={}",
                    message.getDeviceDataId(),
                    message.getDeviceCode()
            );
            return;
        }

        /*
         * 2. 查询设备信息
         *
         * 优先用 deviceData.deviceId。
         * 如果历史数据里 deviceId 为空，再使用 MQ 中的 deviceId。
         * 如果还为空，再使用 deviceCode 兜底。
         */
        DeviceInfo deviceInfo = getDeviceInfo(message, deviceData);

        if (deviceInfo == null) {
            log.warn(
                    "MQ 告警处理失败：未找到设备信息，deviceDataId={}，deviceId={}，deviceCode={}",
                    message.getDeviceDataId(),
                    message.getDeviceId(),
                    message.getDeviceCode()
            );
            return;
        }

        /*
         * 3. 调用告警检测服务
         *
         * 先执行 THRESHOLD 阈值告警，再执行 CONTINUOUS 连续异常告警。
         * 两者独立检测，互不影响。
         */
        List<Long> thresholdAlarmIds = alarmDetectService.detectThresholdAlarms(deviceInfo, deviceData);
        List<Long> continuousAlarmIds = alarmDetectService.detectContinuousAlarms(deviceInfo, deviceData);

        /*
         * 4. 合并所有告警 ID
         */
        List<Long> allAlarmIds = new ArrayList<>();
        if (thresholdAlarmIds != null) {
            allAlarmIds.addAll(thresholdAlarmIds);
        }
        if (continuousAlarmIds != null) {
            allAlarmIds.addAll(continuousAlarmIds);
        }

        if (allAlarmIds.isEmpty()) {
            log.info(
                    "设备数据异步告警检测完成，未触发告警，deviceDataId={}，deviceCode={}",
                    deviceData.getId(),
                    deviceData.getDeviceCode()
            );
            return;
        }

        log.info(
                "设备数据异步告警检测完成，触发或更新告警，deviceDataId={}，deviceCode={}，thresholdAlarms={}，continuousAlarms={}",
                deviceData.getId(),
                deviceData.getDeviceCode(),
                thresholdAlarmIds,
                continuousAlarmIds
        );

        /*
         * 5. 事务提交成功后推送 WebSocket 告警消息
         *
         * 不要在事务提交前推送。
         * 否则前端可能收到推送消息，但告警记录事务还没提交，点击详情查不到数据。
         */
        pushAlarmAfterTransactionCommit(allAlarmIds);
    }

    /**
     * 事务提交成功后推送 WebSocket 告警消息
     *
     * 为什么不在检测完成后直接推送：
     * 告警记录在事务中新增或更新，如果事务还没提交就推送，
     * 前端收到推送后点击详情可能查不到数据。
     */
    private void pushAlarmAfterTransactionCommit(List<Long> alarmIds) {
        if (alarmIds == null || alarmIds.isEmpty()) {
            return;
        }

        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {

                @Override
                public void afterCommit() {
                    alarmPushService.pushAlarmMessages(alarmIds);
                }

                @Override
                public void afterCompletion(int status) {
                    if (status != STATUS_COMMITTED) {
                        log.warn(
                                "告警检测事务未提交成功，取消 WebSocket 推送，alarmIds={}，status={}",
                                alarmIds,
                                status
                        );
                    }
                }
            });
            return;
        }

        /*
         * 理论上不会走到这里，因为 handleDeviceDataReportMessage 有 @Transactional。
         * 这里作为兜底。
         */
        alarmPushService.pushAlarmMessages(alarmIds);
    }

    /**
     * 查询设备信息
     */
    private DeviceInfo getDeviceInfo(DeviceDataReportMessage message, DeviceData deviceData) {
        Long deviceId = deviceData.getDeviceId();

        if (deviceId == null) {
            deviceId = message.getDeviceId();
        }

        if (deviceId != null) {
            DeviceInfo deviceInfo = deviceInfoMapper.selectById(deviceId);
            if (deviceInfo != null) {
                return deviceInfo;
            }
        }

        String deviceCode = deviceData.getDeviceCode();

        if (!StringUtils.hasText(deviceCode)) {
            deviceCode = message.getDeviceCode();
        }

        if (!StringUtils.hasText(deviceCode)) {
            return null;
        }

        return deviceInfoMapper.selectOne(
                new LambdaQueryWrapper<DeviceInfo>()
                        .eq(DeviceInfo::getDeviceCode, deviceCode)
                        .last("LIMIT 1")
        );
    }
}