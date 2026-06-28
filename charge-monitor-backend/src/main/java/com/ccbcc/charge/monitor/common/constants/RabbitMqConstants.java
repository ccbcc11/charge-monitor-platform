package com.ccbcc.charge.monitor.common.constants;

/**
 * RabbitMQ 常量
 */
public final class RabbitMqConstants {

    /**
     * 设备数据告警交换机
     */
    public static final String DEVICE_DATA_ALARM_EXCHANGE = "charge.monitor.device.data.alarm.exchange";

    /**
     * 设备数据告警队列
     */
    public static final String DEVICE_DATA_ALARM_QUEUE = "charge.monitor.device.data.alarm.queue";

    /**
     * 设备数据上报路由键
     */
    public static final String DEVICE_DATA_ALARM_ROUTING_KEY = "charge.monitor.device.data.report";

    private RabbitMqConstants() {
    }
}