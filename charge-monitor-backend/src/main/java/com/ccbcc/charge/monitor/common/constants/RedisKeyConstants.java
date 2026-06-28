package com.ccbcc.charge.monitor.common.constants;

public final class RedisKeyConstants {

    public static final String DEVICE_STATUS_PREFIX = "device:status:";
    public static final String DEVICE_HEARTBEAT_PREFIX = "device:heartbeat:";
    public static final String DEVICE_ONLINE_SET = "device:online:set";
    public static final String DEVICE_ALARM_SET = "device:alarm:set";
    public static final String ALARM_RULE_ENABLED = "alarm:rule:enabled";

    private RedisKeyConstants() {
    }

    public static String deviceStatus(String deviceCode) {
        return DEVICE_STATUS_PREFIX + deviceCode;
    }

    public static String deviceHeartbeat(String deviceCode) {
        return DEVICE_HEARTBEAT_PREFIX + deviceCode;
    }

    /**
     * 启用告警规则缓存前缀
     *
     * 示例：
     * alarm:rule:enabled:THRESHOLD
     * alarm:rule:enabled:OFFLINE
     */
    public static String enabledAlarmRules(String alarmType) {
        return ALARM_RULE_ENABLED + ":" + alarmType;
    }
}
