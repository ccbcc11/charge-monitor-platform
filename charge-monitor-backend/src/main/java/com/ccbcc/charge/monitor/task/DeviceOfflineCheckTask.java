package com.ccbcc.charge.monitor.task;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.ccbcc.charge.monitor.common.constants.RedisKeyConstants;
import com.ccbcc.charge.monitor.config.DeviceOfflineProperties;
import com.ccbcc.charge.monitor.module.alarm.entity.AlarmRecord;
import com.ccbcc.charge.monitor.module.alarm.mapper.AlarmRecordMapper;
import com.ccbcc.charge.monitor.module.device.entity.DeviceInfo;
import com.ccbcc.charge.monitor.module.device.mapper.DeviceInfoMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;

/**
 * 设备离线检测定时任务
 *
 * 核心逻辑：
 * 1. 定时查询所有未删除、未停用设备
 * 2. 优先读取 Redis 中的心跳时间
 * 3. Redis 无心跳时，使用 MySQL device_info.last_heartbeat 兜底
 * 4. 如果当前时间 - 最近心跳时间 > offlineSeconds，则判定设备离线
 * 5. 更新 device_info.online_status = 0
 * 6. 从 Redis device:online:set 中移除设备编号
 * 7. 生成或更新 OFFLINE 离线告警
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DeviceOfflineCheckTask {

    private static final Integer STATUS_OFFLINE = 0;
    private static final Integer RUNNING_STATUS_STOPPED = 0;

    private static final String ALARM_TYPE_OFFLINE = "OFFLINE";
    private static final String ALARM_METRIC_HEARTBEAT = "heartbeat";

    /**
     * 告警等级：1 一般，2 重要，3 严重
     * 离线告警建议设为 2：重要
     */
    private static final Integer ALARM_LEVEL_IMPORTANT = 2;

    /**
     * 告警状态：0 未确认，1 已确认，2 已恢复
     */
    private static final Integer ALARM_STATUS_UNCONFIRMED = 0;
    private static final Integer ALARM_STATUS_CONFIRMED = 1;

    private static final DateTimeFormatter DATE_TIME_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final DeviceOfflineProperties deviceOfflineProperties;
    private final DeviceInfoMapper deviceInfoMapper;
    private final AlarmRecordMapper alarmRecordMapper;
    private final StringRedisTemplate stringRedisTemplate;

    /**
     * fixedDelayString 从 application.yml 读取：
     *
     * charge-monitor:
     *   device:
     *     offline:
     *       fixed-delay: 30000
     *
     * fixedDelay 表示：上一次任务执行完成后，等待 fixed-delay 毫秒再执行下一次。
     */
    @Scheduled(fixedDelayString = "${charge-monitor.device.offline.fixed-delay:30000}")
    public void checkOfflineDevices() {
        if (!deviceOfflineProperties.isEnabled()) {
            log.debug("设备离线检测任务未开启，跳过本次扫描");
            return;
        }

        Long offlineSeconds = deviceOfflineProperties.getOfflineSeconds();
        if (offlineSeconds == null || offlineSeconds <= 0) {
            log.warn("设备离线检测配置 offlineSeconds 不合法：{}，跳过本次扫描", offlineSeconds);
            return;
        }

        LocalDateTime now = LocalDateTime.now();

        List<DeviceInfo> devices = deviceInfoMapper.selectList(
                new LambdaQueryWrapper<DeviceInfo>()
                        .eq(DeviceInfo::getDeleted, 0)
                        .ne(DeviceInfo::getRunningStatus, RUNNING_STATUS_STOPPED)
        );

        if (devices == null || devices.isEmpty()) {
            log.debug("设备离线检测：当前没有需要检测的设备");
            return;
        }

        int offlineCount = 0;

        for (DeviceInfo device : devices) {
            try {
                boolean offline = checkSingleDeviceOffline(device, now, offlineSeconds);
                if (offline) {
                    offlineCount++;
                }
            } catch (Exception e) {
                log.warn("设备离线检测异常，deviceCode={}", device.getDeviceCode(), e);
            }
        }

        log.debug("设备离线检测完成，本次扫描设备数={}，离线处理数={}", devices.size(), offlineCount);
    }

    /**
     * 检测单台设备是否离线
     */
    private boolean checkSingleDeviceOffline(DeviceInfo device, LocalDateTime now, Long offlineSeconds) {
        if (device == null || !StringUtils.hasText(device.getDeviceCode())) {
            return false;
        }

        String deviceCode = device.getDeviceCode();

        LocalDateTime lastHeartbeat = getLastHeartbeat(device);

        /*
         * 如果设备从未上报过心跳：
         * 第一版先不生成离线告警。
         *
         * 原因：
         * 设备刚录入台账但还没正式接入时，如果直接报离线，容易造成误报。
         */
        if (lastHeartbeat == null) {
            log.debug("设备暂无心跳记录，暂不判定离线，deviceCode={}", deviceCode);
            return false;
        }

        long silentSeconds = Duration.between(lastHeartbeat, now).getSeconds();

        if (silentSeconds <= offlineSeconds) {
            return false;
        }

        log.info(
                "检测到设备离线，deviceCode={}，lastHeartbeat={}，silentSeconds={}，offlineSeconds={}",
                deviceCode,
                lastHeartbeat,
                silentSeconds,
                offlineSeconds
        );

        markDeviceOffline(device);
        removeFromOnlineSet(deviceCode);
        saveOrUpdateOfflineAlarm(device, now, silentSeconds, offlineSeconds);
        addToAlarmSet(deviceCode);

        return true;
    }

    /**
     * 获取设备最近心跳时间
     *
     * 优先级：
     * 1. Redis：device:heartbeat:{deviceCode}
     * 2. MySQL：device_info.last_heartbeat
     */
    private LocalDateTime getLastHeartbeat(DeviceInfo device) {
        String deviceCode = device.getDeviceCode();

        try {
            String heartbeatValue = stringRedisTemplate.opsForValue()
                    .get(RedisKeyConstants.deviceHeartbeat(deviceCode));

            LocalDateTime redisHeartbeat = parseHeartbeatTime(heartbeatValue);
            if (redisHeartbeat != null) {
                return redisHeartbeat;
            }
        } catch (Exception e) {
            log.warn("读取 Redis 心跳失败，使用 MySQL lastHeartbeat 兜底，deviceCode={}", deviceCode, e);
        }

        return device.getLastHeartbeat();
    }

    /**
     * 兼容解析心跳时间
     *
     * 支持：
     * 1. yyyy-MM-dd HH:mm:ss
     * 2. LocalDateTime 默认格式：yyyy-MM-ddTHH:mm:ss
     * 3. 毫秒时间戳
     */
    private LocalDateTime parseHeartbeatTime(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }

        String text = value.trim();

        try {
            return LocalDateTime.parse(text, DATE_TIME_FORMATTER);
        } catch (DateTimeParseException ignored) {
            // 尝试下一种格式
        }

        try {
            return LocalDateTime.parse(text);
        } catch (DateTimeParseException ignored) {
            // 尝试下一种格式
        }

        try {
            long timestamp = Long.parseLong(text);
            return LocalDateTime.ofInstant(
                    java.time.Instant.ofEpochMilli(timestamp),
                    java.time.ZoneId.systemDefault()
            );
        } catch (NumberFormatException ignored) {
            // 无法解析
        }

        log.warn("无法解析设备心跳时间：{}", value);
        return null;
    }

    /**
     * 更新设备为离线状态
     */
    private void markDeviceOffline(DeviceInfo device) {
        /*
         * 如果设备本来就是离线状态，可以不重复 update。
         * 但是即使重复执行，影响也不大。
         */
        if (STATUS_OFFLINE.equals(device.getOnlineStatus())) {
            return;
        }

        DeviceInfo updateEntity = new DeviceInfo();
        updateEntity.setOnlineStatus(STATUS_OFFLINE);
        updateEntity.setUpdateTime(LocalDateTime.now());

        deviceInfoMapper.update(
                updateEntity,
                new LambdaUpdateWrapper<DeviceInfo>()
                        .eq(DeviceInfo::getId, device.getId())
        );
    }

    /**
     * 从 Redis 在线设备集合移除
     */
    private void removeFromOnlineSet(String deviceCode) {
        try {
            stringRedisTemplate.opsForSet()
                    .remove(RedisKeyConstants.DEVICE_ONLINE_SET, deviceCode);
        } catch (Exception e) {
            log.warn("从 Redis 在线集合移除设备失败，deviceCode={}", deviceCode, e);
        }
    }

    /**
     * 加入 Redis 当前告警设备集合
     */
    private void addToAlarmSet(String deviceCode) {
        try {
            stringRedisTemplate.opsForSet()
                    .add(RedisKeyConstants.DEVICE_ALARM_SET, deviceCode);
        } catch (Exception e) {
            log.warn("写入 Redis 告警设备集合失败，deviceCode={}", deviceCode, e);
        }
    }

    /**
     * 新增或更新 OFFLINE 离线告警
     *
     * 去重维度：
     * 同一设备 + OFFLINE + heartbeat + alarm_status in (0, 1)
     */
    private void saveOrUpdateOfflineAlarm(
            DeviceInfo device,
            LocalDateTime now,
            long silentSeconds,
            Long offlineSeconds
    ) {
        AlarmRecord existing = alarmRecordMapper.selectOne(
                new LambdaQueryWrapper<AlarmRecord>()
                        .eq(AlarmRecord::getDeleted, 0)
                        .eq(AlarmRecord::getDeviceId, device.getId())
                        .eq(AlarmRecord::getAlarmType, ALARM_TYPE_OFFLINE)
                        .eq(AlarmRecord::getAlarmMetric, ALARM_METRIC_HEARTBEAT)
                        .in(AlarmRecord::getAlarmStatus, ALARM_STATUS_UNCONFIRMED, ALARM_STATUS_CONFIRMED)
                        .last("LIMIT 1")
        );

        String message = String.format(
                "设备超过 %d 秒未上报心跳，当前已静默 %d 秒，判定为离线",
                offlineSeconds,
                silentSeconds
        );

        if (existing == null) {
            AlarmRecord alarmRecord = new AlarmRecord();
            alarmRecord.setAlarmNo(generateAlarmNo());
            alarmRecord.setDeviceId(device.getId());
            alarmRecord.setDeviceCode(device.getDeviceCode());
            alarmRecord.setAlarmType(ALARM_TYPE_OFFLINE);
            alarmRecord.setAlarmMetric(ALARM_METRIC_HEARTBEAT);
            alarmRecord.setAlarmLevel(ALARM_LEVEL_IMPORTANT);
            alarmRecord.setCurrentValue(BigDecimal.valueOf(silentSeconds));
            alarmRecord.setThresholdValue(BigDecimal.valueOf(offlineSeconds));
            alarmRecord.setAlarmMessage(message);
            alarmRecord.setAlarmStatus(ALARM_STATUS_UNCONFIRMED);
            alarmRecord.setFirstTime(now);
            alarmRecord.setLastTime(now);
            alarmRecord.setAlarmCount(1);
            alarmRecord.setWorkOrderGenerated(0);
            alarmRecord.setDedupKey(buildOfflineDedupKey(device.getId()));
            alarmRecord.setCreateTime(now);
            alarmRecord.setUpdateTime(now);
            alarmRecord.setDeleted(0);

            alarmRecordMapper.insert(alarmRecord);
            return;
        }

        existing.setCurrentValue(BigDecimal.valueOf(silentSeconds));
        existing.setThresholdValue(BigDecimal.valueOf(offlineSeconds));
        existing.setAlarmMessage(message);
        existing.setLastTime(now);
        existing.setAlarmCount(existing.getAlarmCount() == null ? 1 : existing.getAlarmCount() + 1);
        existing.setUpdateTime(now);

        alarmRecordMapper.updateById(existing);
    }

    /**
     * 构造离线告警去重 Key
     */
    private String buildOfflineDedupKey(Long deviceId) {
        return deviceId + ":" + ALARM_TYPE_OFFLINE + ":" + ALARM_METRIC_HEARTBEAT;
    }

    /**
     * 生成告警编号
     *
     * 示例：
     * AL20260628143030123456
     */
    private String generateAlarmNo() {
        return "AL"
                + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"))
                + String.valueOf(System.nanoTime()).substring(7);
    }
}