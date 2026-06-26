package com.ccbcc.charge.monitor.module.report.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ccbcc.charge.monitor.module.alarm.entity.AlarmRecord;
import com.ccbcc.charge.monitor.module.alarm.mapper.AlarmRecordMapper;
import com.ccbcc.charge.monitor.module.device.entity.DeviceInfo;
import com.ccbcc.charge.monitor.module.device.mapper.DeviceInfoMapper;
import com.ccbcc.charge.monitor.module.report.service.ReportService;
import com.ccbcc.charge.monitor.module.report.vo.OverviewVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

/**
 * 报表服务实现类
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ReportServiceImpl implements ReportService {

    private final DeviceInfoMapper deviceInfoMapper;
    private final AlarmRecordMapper alarmRecordMapper;
    private final RedisTemplate<String, Object> redisTemplate;

    /**
     * Redis Key：在线设备集合
     */
    private static final String DEVICE_ONLINE_SET_KEY = "device:online:set";

    /**
     * Redis Key：当前存在未恢复告警的设备集合
     */
    private static final String DEVICE_ALARM_SET_KEY = "device:alarm:set";

    /**
     * 在线状态：在线
     */
    private static final Integer ONLINE_STATUS_ONLINE = 1;

    /**
     * 告警状态：未确认
     */
    private static final Integer ALARM_STATUS_UNACK = 0;

    /**
     * 告警状态：已确认
     */
    private static final Integer ALARM_STATUS_ACKED = 1;

    /**
     * 告警等级：严重
     */
    private static final Integer ALARM_LEVEL_SERIOUS = 3;

    @Override
    public OverviewVO getOverview() {

        /*
         * 1. 设备总数
         */
        Long deviceTotal = countDeviceTotal();

        /*
         * 2. 在线设备数
         *
         * 优先读 Redis 的 device:online:set。
         * Redis 不可用或无数据时，从 MySQL 的 online_status 兜底统计。
         */
        Long onlineCount = countOnlineDevice();

        /*
         * 3. 离线设备数
         */
        Long offlineCount = Math.max(deviceTotal - onlineCount, 0L);

        /*
         * 4. 当前异常设备数
         *
         * 优先读 Redis 的 device:alarm:set。
         * Redis 不可用或无数据时，从 MySQL 当前未恢复告警里按设备去重统计。
         */
        Long abnormalDeviceCount = countAbnormalDevice();

        /*
         * 5. 今日告警数
         */
        Long todayAlarmCount = countTodayAlarm();

        /*
         * 6. 今日严重告警数
         */
        Long seriousAlarmCount = countTodaySeriousAlarm();

        /*
         * 7. 未处理告警数
         *
         * MVP 定义：
         * alarm_status in (0, 1)
         */
        Long unhandledAlarmCount = countUnhandledAlarm();

        /*
         * 8. 在线率
         */
        BigDecimal onlineRate = calculateOnlineRate(onlineCount, deviceTotal);

        return new OverviewVO()
                .setDeviceTotal(deviceTotal)
                .setOnlineCount(onlineCount)
                .setOfflineCount(offlineCount)
                .setAbnormalDeviceCount(abnormalDeviceCount)
                .setTodayAlarmCount(todayAlarmCount)
                .setSeriousAlarmCount(seriousAlarmCount)
                .setUnhandledAlarmCount(unhandledAlarmCount)
                .setOnlineRate(onlineRate);
    }

    /**
     * 统计设备总数
     */
    private Long countDeviceTotal() {
        return deviceInfoMapper.selectCount(new LambdaQueryWrapper<DeviceInfo>());
    }

    /**
     * 统计在线设备数
     */
    private Long countOnlineDevice() {
        Long redisOnlineCount = getRedisSetSize(DEVICE_ONLINE_SET_KEY);

        if (redisOnlineCount != null) {
            return redisOnlineCount;
        }

        return deviceInfoMapper.selectCount(
                new LambdaQueryWrapper<DeviceInfo>()
                        .eq(DeviceInfo::getOnlineStatus, ONLINE_STATUS_ONLINE)
        );
    }

    /**
     * 统计当前异常设备数
     */
    private Long countAbnormalDevice() {
        Long redisAbnormalCount = getRedisSetSize(DEVICE_ALARM_SET_KEY);

        if (redisAbnormalCount != null) {
            return redisAbnormalCount;
        }

        /*
         * Redis 不可用时，使用 MySQL 兜底。
         *
         * 查询所有未恢复告警，再按 deviceCode 去重。
         * MVP 数据量较小，这样写简单直观。
         * 后续数据量大时可以改为 Mapper 自定义 count distinct SQL。
         */
        List<AlarmRecord> records = alarmRecordMapper.selectList(
                new LambdaQueryWrapper<AlarmRecord>()
                        .select(AlarmRecord::getDeviceCode)
                        .in(AlarmRecord::getAlarmStatus, ALARM_STATUS_UNACK, ALARM_STATUS_ACKED)
        );

        return records.stream()
                .map(AlarmRecord::getDeviceCode)
                .filter(deviceCode -> deviceCode != null && !deviceCode.isBlank())
                .distinct()
                .count();
    }

    /**
     * 统计今日告警数
     */
    private Long countTodayAlarm() {
        LocalDateTime start = LocalDateTime.of(LocalDateTime.now().toLocalDate(), LocalTime.MIN);
        LocalDateTime end = LocalDateTime.of(LocalDateTime.now().toLocalDate(), LocalTime.MAX);

        return alarmRecordMapper.selectCount(
                new LambdaQueryWrapper<AlarmRecord>()
                        .ge(AlarmRecord::getCreateTime, start)
                        .le(AlarmRecord::getCreateTime, end)
        );
    }

    /**
     * 统计今日严重告警数
     */
    private Long countTodaySeriousAlarm() {
        LocalDateTime start = LocalDateTime.of(LocalDateTime.now().toLocalDate(), LocalTime.MIN);
        LocalDateTime end = LocalDateTime.of(LocalDateTime.now().toLocalDate(), LocalTime.MAX);

        return alarmRecordMapper.selectCount(
                new LambdaQueryWrapper<AlarmRecord>()
                        .eq(AlarmRecord::getAlarmLevel, ALARM_LEVEL_SERIOUS)
                        .ge(AlarmRecord::getCreateTime, start)
                        .le(AlarmRecord::getCreateTime, end)
        );
    }

    /**
     * 统计未处理告警数
     */
    private Long countUnhandledAlarm() {
        return alarmRecordMapper.selectCount(
                new LambdaQueryWrapper<AlarmRecord>()
                        .in(AlarmRecord::getAlarmStatus, ALARM_STATUS_UNACK, ALARM_STATUS_ACKED)
        );
    }

    /**
     * 获取 Redis Set 大小
     *
     * @param key Redis Key
     * @return Set 大小；Redis 不可用时返回 null
     */
    private Long getRedisSetSize(String key) {
        try {
            return redisTemplate.opsForSet().size(key);
        } catch (Exception e) {
            log.warn("读取Redis集合大小失败，key={}", key, e);
            return null;
        }
    }

    /**
     * 计算在线率
     */
    private BigDecimal calculateOnlineRate(Long onlineCount, Long deviceTotal) {
        if (deviceTotal == null || deviceTotal == 0) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }

        return BigDecimal.valueOf(onlineCount)
                .multiply(BigDecimal.valueOf(100))
                .divide(BigDecimal.valueOf(deviceTotal), 2, RoundingMode.HALF_UP);
    }
}