package com.ccbcc.charge.monitor.module.alarm.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ccbcc.charge.monitor.common.constants.RedisKeyConstants;
import com.ccbcc.charge.monitor.module.alarm.entity.AlarmRecord;
import com.ccbcc.charge.monitor.module.alarm.entity.AlarmRule;
import com.ccbcc.charge.monitor.module.alarm.mapper.AlarmRecordMapper;
import com.ccbcc.charge.monitor.module.alarm.mapper.AlarmRuleMapper;
import com.ccbcc.charge.monitor.module.alarm.service.AlarmDetectService;
import com.ccbcc.charge.monitor.module.alarm.service.AlarmRuleCacheService;
import com.ccbcc.charge.monitor.module.device.entity.DeviceData;
import com.ccbcc.charge.monitor.module.device.entity.DeviceInfo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 告警检测服务实现类
 *
 * 当前已实现：
 * 1. THRESHOLD 阈值告警：单次数据判断，从 Redis 缓存读取规则
 * 2. CONTINUOUS 连续异常告警：基于 Redis List 滑动窗口，统计窗口内命中次数
 *
 * 暂不处理：
 * 1. OFFLINE 离线告警：当前仍由 DeviceOfflineCheckTask 处理
 * 2. FLUCTUATION 波动异常：后续再扩展
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AlarmDetectServiceImpl implements AlarmDetectService {

    private static final String ALARM_TYPE_THRESHOLD = "THRESHOLD";
    private static final String ALARM_TYPE_CONTINUOUS = "CONTINUOUS";

    /**
     * 告警状态：0 未确认，1 已确认，2 已恢复
     */
    private static final Integer STATUS_UNCONFIRMED = 0;
    private static final Integer STATUS_CONFIRMED = 1;

    /**
     * 规则启用状态
     */
    private static final Integer RULE_ENABLED = 1;

    /**
     * 连续异常窗口 TTL
     *
     * 设备停止上报后，窗口 key 自动过期，避免 Redis 内存泄漏。
     * 每次 LPUSH 时会通过 EXPIRE 续期。
     */
    private static final Duration WINDOW_TTL = Duration.ofMinutes(10);

    private final AlarmRuleCacheService alarmRuleCacheService;
    private final AlarmRecordMapper alarmRecordMapper;
    private final AlarmRuleMapper alarmRuleMapper;
    private final StringRedisTemplate stringRedisTemplate;

    @Override
    public List<Long> detectThresholdAlarms(DeviceInfo deviceInfo, DeviceData deviceData) {
        List<Long> alarmIds = new ArrayList<>();

        if (deviceInfo == null || deviceData == null) {
            return alarmIds;
        }

        /*
         * 从 Redis 缓存获取启用的 THRESHOLD 规则。
         *
         * Redis 未命中时，会自动查询 MySQL 并回写 Redis。
         *
         * 表里可以保留 OFFLINE、CONTINUOUS 等规则，
         * 但这里暂时不处理，避免不同告警类型逻辑混在一起。
         */
        List<AlarmRule> rules = alarmRuleCacheService.getEnabledThresholdRules();

        if (rules == null || rules.isEmpty()) {
            log.debug("当前没有启用的 THRESHOLD 告警规则");
            return alarmIds;
        }

        for (AlarmRule rule : rules) {
            try {
                Long alarmId = detectSingleThresholdRule(deviceInfo, deviceData, rule);
                if (alarmId != null) {
                    alarmIds.add(alarmId);
                }
            } catch (Exception e) {
                log.warn(
                        "执行告警规则失败，ruleCode={}，deviceCode={}",
                        rule.getRuleCode(),
                        deviceInfo.getDeviceCode(),
                        e
                );
            }
        }

        return alarmIds;
    }

    /**
     * 检测单条阈值规则
     */
    private Long detectSingleThresholdRule(DeviceInfo deviceInfo,
                                           DeviceData deviceData,
                                           AlarmRule rule) {
        if (!isRuleApplicableToDevice(deviceInfo, rule)) {
            return null;
        }

        BigDecimal currentValue = getMetricValue(deviceData, rule.getMetricName());

        if (currentValue == null) {
            log.debug(
                    "规则指标无当前值，跳过检测，ruleCode={}，metricName={}，deviceCode={}",
                    rule.getRuleCode(),
                    rule.getMetricName(),
                    deviceInfo.getDeviceCode()
            );
            return null;
        }

        if (rule.getThresholdValue() == null) {
            log.warn(
                    "规则阈值为空，跳过检测，ruleCode={}，metricName={}",
                    rule.getRuleCode(),
                    rule.getMetricName()
            );
            return null;
        }

        if (!isTriggered(currentValue, rule.getOperator(), rule.getThresholdValue())) {
            return null;
        }

        String alarmMessage = buildAlarmMessage(rule, currentValue);

        return saveOrUpdateAlarmRecord(
                deviceInfo,
                rule,
                currentValue,
                rule.getThresholdValue(),
                alarmMessage
        );
    }

    /**
     * 判断规则是否适用于当前设备
     *
     * rule.deviceType 为空，表示适用于全部设备。
     * rule.deviceType 不为空，则必须和 deviceInfo.deviceType 一致。
     */
    private boolean isRuleApplicableToDevice(DeviceInfo deviceInfo, AlarmRule rule) {
        if (!StringUtils.hasText(rule.getDeviceType())) {
            return true;
        }

        if (!StringUtils.hasText(deviceInfo.getDeviceType())) {
            return false;
        }

        return rule.getDeviceType().equalsIgnoreCase(deviceInfo.getDeviceType());
    }

    /**
     * 根据规则指标名，从本次上报数据中取当前值
     */
    private BigDecimal getMetricValue(DeviceData deviceData, String metricName) {
        if (!StringUtils.hasText(metricName)) {
            return null;
        }

        return switch (metricName) {
            case "temperature" -> deviceData.getTemperature();
            case "voltage" -> deviceData.getVoltage();

            /*
             * 数据库中建议使用 current 表示电流指标；
             * Java 字段使用 currentValue，避免 current 关键字/语义冲突。
             */
            case "current", "current_value" -> deviceData.getCurrentValue();

            case "power" -> deviceData.getPower();

            /*
             * 数据库规则中使用 network_delay；
             * Java 字段使用 networkDelay。
             */
            case "network_delay", "networkDelay" -> {
                Integer networkDelay = deviceData.getNetworkDelay();
                yield networkDelay == null ? null : BigDecimal.valueOf(networkDelay);
            }

            default -> {
                log.warn("暂不支持的告警指标 metricName={}", metricName);
                yield null;
            }
        };
    }

    /**
     * 判断当前值是否命中规则
     */
    private boolean isTriggered(BigDecimal currentValue,
                                String operator,
                                BigDecimal thresholdValue) {
        if (currentValue == null || thresholdValue == null || !StringUtils.hasText(operator)) {
            return false;
        }

        int compare = currentValue.compareTo(thresholdValue);

        return switch (operator.trim()) {
            case ">" -> compare > 0;
            case ">=" -> compare >= 0;
            case "<" -> compare < 0;
            case "<=" -> compare <= 0;
            case "=", "==" -> compare == 0;
            case "!=" -> compare != 0;
            default -> {
                log.warn("暂不支持的告警运算符 operator={}", operator);
                yield false;
            }
        };
    }

    /**
     * 新增或更新告警记录
     *
     * 去重维度：
     * 同一设备 + 同一告警类型 + 同一告警指标 + 未恢复状态
     */
    private Long saveOrUpdateAlarmRecord(DeviceInfo deviceInfo,
                                         AlarmRule rule,
                                         BigDecimal currentValue,
                                         BigDecimal thresholdValue,
                                         String alarmMessage) {
        LocalDateTime now = LocalDateTime.now();

        AlarmRecord existing = alarmRecordMapper.selectOne(
                new LambdaQueryWrapper<AlarmRecord>()
                        .eq(AlarmRecord::getDeviceId, deviceInfo.getId())
                        .eq(AlarmRecord::getAlarmType, rule.getAlarmType())
                        .eq(AlarmRecord::getAlarmMetric, rule.getMetricName())
                        .in(AlarmRecord::getAlarmStatus, STATUS_UNCONFIRMED, STATUS_CONFIRMED)
                        .last("LIMIT 1")
        );

        if (existing == null) {
            AlarmRecord record = new AlarmRecord()
                    .setAlarmNo(generateAlarmNo())
                    .setDeviceId(deviceInfo.getId())
                    .setDeviceCode(deviceInfo.getDeviceCode())
                    .setAlarmType(rule.getAlarmType())
                    .setAlarmMetric(rule.getMetricName())
                    .setAlarmLevel(rule.getAlarmLevel())
                    .setCurrentValue(currentValue)
                    .setThresholdValue(thresholdValue)
                    .setAlarmMessage(alarmMessage)
                    .setAlarmStatus(STATUS_UNCONFIRMED)
                    .setFirstTime(now)
                    .setLastTime(now)
                    .setRecoverTime(null)
                    .setAlarmCount(1)
                    .setWorkOrderGenerated(0)
                    .setDedupKey(buildDedupKey(deviceInfo.getDeviceCode(), rule.getAlarmType(), rule.getMetricName()))
                    .setCreateTime(now)
                    .setUpdateTime(now)
                    .setDeleted(0);

            alarmRecordMapper.insert(record);

            addDeviceToAlarmSet(deviceInfo.getDeviceCode());

            log.info(
                    "新增动态规则告警，ruleCode={}，deviceCode={}，metric={}，currentValue={}，threshold={}",
                    rule.getRuleCode(),
                    deviceInfo.getDeviceCode(),
                    rule.getMetricName(),
                    currentValue,
                    thresholdValue
            );

            return record.getId();
        }

        Integer oldCount = existing.getAlarmCount() == null ? 0 : existing.getAlarmCount();

        existing.setCurrentValue(currentValue)
                .setThresholdValue(thresholdValue)
                .setAlarmMessage(alarmMessage)
                .setAlarmLevel(rule.getAlarmLevel())
                .setLastTime(now)
                .setAlarmCount(oldCount + 1)
                .setUpdateTime(now);

        alarmRecordMapper.updateById(existing);

        addDeviceToAlarmSet(deviceInfo.getDeviceCode());

        log.debug(
                "更新已有动态规则告警，alarmId={}，ruleCode={}，deviceCode={}，metric={}，alarmCount={}",
                existing.getId(),
                rule.getRuleCode(),
                deviceInfo.getDeviceCode(),
                rule.getMetricName(),
                existing.getAlarmCount()
        );

        return existing.getId();
    }

    /**
     * 构造告警文案
     */
    private String buildAlarmMessage(AlarmRule rule, BigDecimal currentValue) {
        String metricName = rule.getMetricName();
        String operator = rule.getOperator();
        BigDecimal thresholdValue = rule.getThresholdValue();

        String metricText = switch (metricName) {
            case "temperature" -> "设备温度";
            case "voltage" -> "设备电压";
            case "current", "current_value" -> "设备电流";
            case "power" -> "设备功率";
            case "network_delay", "networkDelay" -> "网络延迟";
            default -> metricName;
        };

        String unit = switch (metricName) {
            case "temperature" -> "℃";
            case "voltage" -> "V";
            case "current", "current_value" -> "A";
            case "power" -> "kW";
            case "network_delay", "networkDelay" -> "ms";
            default -> "";
        };

        return String.format(
                "%s触发%s，当前值%s%s，规则：%s %s %s%s",
                metricText,
                rule.getRuleName(),
                currentValue,
                unit,
                metricName,
                operator,
                thresholdValue,
                unit
        );
    }

    /**
     * 写入 Redis 当前告警设备集合
     */
    private void addDeviceToAlarmSet(String deviceCode) {
        try {
            stringRedisTemplate.opsForSet()
                    .add(RedisKeyConstants.DEVICE_ALARM_SET, deviceCode);
        } catch (Exception e) {
            log.warn("写入 Redis 告警设备集合失败，deviceCode={}", deviceCode, e);
        }
    }

    /**
     * 构造告警去重键
     */
    private String buildDedupKey(String deviceCode, String alarmType, String alarmMetric) {
        return deviceCode + ":" + alarmType + ":" + alarmMetric;
    }

    /**
     * 生成告警编号
     *
     * 示例：
     * AL202606281530001234567890
     */
    private String generateAlarmNo() {
        String timePart = LocalDateTime.now()
                .format(DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS"));

        int randomPart = ThreadLocalRandom.current().nextInt(1000, 10000);

        return "AL" + timePart + randomPart;
    }

    // ==================== CONTINUOUS 连续异常告警 ====================

    @Override
    public List<Long> detectContinuousAlarms(DeviceInfo deviceInfo, DeviceData deviceData) {
        List<Long> alarmIds = new ArrayList<>();

        if (deviceInfo == null || deviceData == null) {
            return alarmIds;
        }

        /*
         * 第一步先直接从 MySQL 查 CONTINUOUS 规则。
         *
         * 原因：
         * 目前 CONTINUOUS 规则数量很少（通常 1~2 条），
         * 不需要像 THRESHOLD 那样引入缓存，避免增加复杂度。
         * 等后续规则数量增多时再考虑缓存。
         */
        List<AlarmRule> rules = alarmRuleMapper.selectList(
                new LambdaQueryWrapper<AlarmRule>()
                        .eq(AlarmRule::getEnabled, RULE_ENABLED)
                        .eq(AlarmRule::getAlarmType, ALARM_TYPE_CONTINUOUS)
                        .orderByAsc(AlarmRule::getId)
        );

        if (rules == null || rules.isEmpty()) {
            log.debug("当前没有启用的 CONTINUOUS 告警规则");
            return alarmIds;
        }

        for (AlarmRule rule : rules) {
            try {
                Long alarmId = detectSingleContinuousRule(deviceInfo, deviceData, rule);
                if (alarmId != null) {
                    alarmIds.add(alarmId);
                }
            } catch (Exception e) {
                log.warn(
                        "执行连续异常告警规则失败，ruleCode={}，deviceCode={}",
                        rule.getRuleCode(),
                        deviceInfo.getDeviceCode(),
                        e
                );
            }
        }

        return alarmIds;
    }

    @Override
    public void clearContinuousWindow(String deviceCode, String alarmMetric) {
        if (!StringUtils.hasText(deviceCode) || !StringUtils.hasText(alarmMetric)) {
            return;
        }

        /*
         * 连续异常告警的 ruleCode 无法仅凭 alarmMetric 确定。
         * 因此这里遍历所有启用的 CONTINUOUS 规则，找到匹配指标的规则后删除对应窗口。
         */
        List<AlarmRule> rules = alarmRuleMapper.selectList(
                new LambdaQueryWrapper<AlarmRule>()
                        .eq(AlarmRule::getEnabled, RULE_ENABLED)
                        .eq(AlarmRule::getAlarmType, ALARM_TYPE_CONTINUOUS)
        );

        if (rules == null || rules.isEmpty()) {
            return;
        }

        for (AlarmRule rule : rules) {
            if (!alarmMetric.equals(rule.getMetricName())) {
                continue;
            }

            String windowKey = RedisKeyConstants.continuousWindow(deviceCode, rule.getRuleCode());

            try {
                Boolean deleted = stringRedisTemplate.delete(windowKey);
                log.info(
                        "清理连续异常滑动窗口，deviceCode={}，ruleCode={}，key={}，deleted={}",
                        deviceCode,
                        rule.getRuleCode(),
                        windowKey,
                        deleted
                );
            } catch (Exception e) {
                log.warn("清理连续异常滑动窗口失败，key={}", windowKey, e);
            }
        }
    }

    /**
     * 检测单条连续异常规则
     */
    private Long detectSingleContinuousRule(DeviceInfo deviceInfo,
                                             DeviceData deviceData,
                                             AlarmRule rule) {
        if (!isRuleApplicableToDevice(deviceInfo, rule)) {
            return null;
        }

        BigDecimal currentValue = getMetricValue(deviceData, rule.getMetricName());

        /*
         * 指标值为 null 表示本次未上报该指标。
         *
         * 注意：此时不向窗口 push 任何值。
         * "没数据" ≠ "数据正常"，不能当成 0（未命中）处理。
         */
        if (currentValue == null) {
            log.debug(
                    "连续异常规则指标无当前值，跳过本次窗口更新，ruleCode={}，metricName={}，deviceCode={}",
                    rule.getRuleCode(),
                    rule.getMetricName(),
                    deviceInfo.getDeviceCode()
            );
            return null;
        }

        if (rule.getThresholdValue() == null) {
            log.warn(
                    "连续异常规则阈值为空，跳过检测，ruleCode={}，metricName={}",
                    rule.getRuleCode(),
                    rule.getMetricName()
            );
            return null;
        }

        /*
         * 判断本次是否命中规则（1=命中，0=未命中）
         */
        boolean hit = isTriggered(currentValue, rule.getOperator(), rule.getThresholdValue());
        int hitValue = hit ? 1 : 0;

        /*
         * 更新 Redis 滑动窗口
         */
        int windowSize = rule.getWindowSize() != null ? rule.getWindowSize() : 5;
        int triggerCount = rule.getTriggerCount() != null ? rule.getTriggerCount() : 3;

        long hitCount = updateSlidingWindow(deviceInfo.getDeviceCode(), rule.getRuleCode(), hitValue, windowSize);

        /*
         * 窗口数据不足时，不触发告警。
         *
         * 例如 window_size=5，trigger_count=3：
         * 设备刚上线窗口只有 2 条数据，此时 3 >= 2 永远为 false。
         * 这是正确的行为——冷启动阶段数据不够，不能判定为连续异常。
         */
        if (hitCount < triggerCount) {
            log.debug(
                    "连续异常未达触发条件，ruleCode={}，deviceCode={}，hitCount={}，triggerCount={}",
                    rule.getRuleCode(),
                    deviceInfo.getDeviceCode(),
                    hitCount,
                    triggerCount
            );
            return null;
        }

        String alarmMessage = buildContinuousAlarmMessage(rule, currentValue, hitCount, windowSize);

        return saveOrUpdateAlarmRecord(
                deviceInfo,
                rule,
                currentValue,
                rule.getThresholdValue(),
                alarmMessage
        );
    }

    /**
     * 更新 Redis 滑动窗口
     *
     * 使用 LPUSH + LTRIM 实现固定大小窗口。
     * 每次操作后重置 TTL，防止设备停报后 key 泄漏。
     *
     * @return 窗口中值为 1 的数量（即最近 window_size 次中命中的次数）
     */
    private long updateSlidingWindow(String deviceCode, String ruleCode, int hitValue, int windowSize) {
        String windowKey = RedisKeyConstants.continuousWindow(deviceCode, ruleCode);

        try {
            String hitStr = String.valueOf(hitValue);

            /*
             * 使用管道减少网络往返，同时保证 TTL 重置。
             */
            stringRedisTemplate.opsForList().leftPush(windowKey, hitStr);
            stringRedisTemplate.opsForList().trim(windowKey, 0, windowSize - 1);
            stringRedisTemplate.expire(windowKey, WINDOW_TTL);

            /*
             * 统计窗口中值为 1 的数量
             */
            List<String> window = stringRedisTemplate.opsForList().range(windowKey, 0, -1);

            if (window == null || window.isEmpty()) {
                return 0;
            }

            return window.stream().filter("1"::equals).count();

        } catch (Exception e) {
            log.warn(
                    "更新连续异常滑动窗口失败，deviceCode={}，ruleCode={}，hitValue={}",
                    deviceCode,
                    ruleCode,
                    hitValue,
                    e
            );
            return 0;
        }
    }

    /**
     * 构造连续异常告警文案
     */
    private String buildContinuousAlarmMessage(AlarmRule rule,
                                                BigDecimal currentValue,
                                                long hitCount,
                                                int windowSize) {
        String metricText = switch (rule.getMetricName()) {
            case "temperature" -> "设备温度";
            case "voltage" -> "设备电压";
            case "current", "current_value" -> "设备电流";
            case "power" -> "设备功率";
            case "network_delay", "networkDelay" -> "网络延迟";
            default -> rule.getMetricName();
        };

        String unit = switch (rule.getMetricName()) {
            case "temperature" -> "℃";
            case "voltage" -> "V";
            case "current", "current_value" -> "A";
            case "power" -> "kW";
            case "network_delay", "networkDelay" -> "ms";
            default -> "";
        };

        return String.format(
                "%s连续异常触发%s，当前值%s%s，最近%d次中有%d次超过阈值%s%s",
                metricText,
                rule.getRuleName(),
                currentValue,
                unit,
                windowSize,
                hitCount,
                rule.getThresholdValue(),
                unit
        );
    }
}