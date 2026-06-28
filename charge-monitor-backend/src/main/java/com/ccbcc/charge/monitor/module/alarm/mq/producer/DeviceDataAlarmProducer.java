package com.ccbcc.charge.monitor.module.alarm.mq.producer;

import com.ccbcc.charge.monitor.common.constants.RabbitMqConstants;
import com.ccbcc.charge.monitor.module.alarm.mq.message.DeviceDataReportMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

/**
 * 设备数据告警消息生产者
 *
 * 作用：
 * 设备运行数据入库成功后，发送一条 MQ 消息，
 * 后续由 DeviceDataAlarmConsumer 异步执行告警检测。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DeviceDataAlarmProducer {

    private final RabbitTemplate rabbitTemplate;

    /**
     * 发送设备数据上报消息
     *
     * @param message 设备数据上报消息
     */
    public void sendDeviceDataReportMessage(DeviceDataReportMessage message) {
        if (message == null) {
            log.warn("设备数据上报 MQ 消息为空，取消发送");
            return;
        }

        if (message.getDeviceDataId() == null) {
            log.warn("设备数据上报 MQ 消息缺少 deviceDataId，取消发送，message={}", message);
            return;
        }

        try {
            rabbitTemplate.convertAndSend(
                    RabbitMqConstants.DEVICE_DATA_ALARM_EXCHANGE,
                    RabbitMqConstants.DEVICE_DATA_ALARM_ROUTING_KEY,
                    message
            );

            log.info(
                    "发送设备数据上报 MQ 消息成功，deviceDataId={}，deviceId={}，deviceCode={}，reportTime={}",
                    message.getDeviceDataId(),
                    message.getDeviceId(),
                    message.getDeviceCode(),
                    message.getReportTime()
            );
        } catch (Exception e) {
            log.error(
                    "发送设备数据上报 MQ 消息失败，deviceDataId={}，deviceCode={}",
                    message.getDeviceDataId(),
                    message.getDeviceCode(),
                    e
            );
        }
    }
}