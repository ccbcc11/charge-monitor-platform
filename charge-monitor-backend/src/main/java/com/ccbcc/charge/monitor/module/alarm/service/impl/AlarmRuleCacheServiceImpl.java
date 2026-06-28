package com.ccbcc.charge.monitor.module.alarm.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ccbcc.charge.monitor.common.constants.RedisKeyConstants;
import com.ccbcc.charge.monitor.module.alarm.entity.AlarmRule;
import com.ccbcc.charge.monitor.module.alarm.mapper.AlarmRuleMapper;
import com.ccbcc.charge.monitor.module.alarm.service.AlarmRuleCacheService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.util.Collections;
import java.util.List;

/**
 * 告警规则缓存服务实现类
 *
 * 当前缓存：
 * 1. 启用的 THRESHOLD 阈值规则
 *
 * 缓存策略：
 * 1. 优先读 Redis
 * 2. Redis 未命中时读 MySQL
 * 3. MySQL 查询结果写入 Redis
 * 4. 规则新增、修改、删除、启停时删除缓存
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AlarmRuleCacheServiceImpl implements AlarmRuleCacheService {

    private static final String ALARM_TYPE_THRESHOLD = "THRESHOLD";

    private static final Integer RULE_ENABLED = 1;

    /**
     * 缓存过期时间
     *
     * 即使规则管理接口忘记清缓存，最多 30 分钟后也会自动重新加载。
     */
    private static final Duration CACHE_TTL = Duration.ofMinutes(30);

    private final AlarmRuleMapper alarmRuleMapper;
    private final StringRedisTemplate stringRedisTemplate;
    private final ObjectMapper objectMapper;

    @Override
    public List<AlarmRule> getEnabledThresholdRules() {
        String redisKey = RedisKeyConstants.enabledAlarmRules(ALARM_TYPE_THRESHOLD);

        /*
         * 1. 先查 Redis
         */
        try {
            String cacheValue = stringRedisTemplate.opsForValue().get(redisKey);

            if (StringUtils.hasText(cacheValue)) {
                List<AlarmRule> cachedRules = objectMapper.readValue(
                        cacheValue,
                        new TypeReference<List<AlarmRule>>() {
                        }
                );

                log.debug("命中 Redis 告警规则缓存，key={}，ruleCount={}", redisKey, cachedRules.size());

                return cachedRules;
            }
        } catch (Exception e) {
            log.warn("读取 Redis 告警规则缓存失败，key={}，将回退查询 MySQL", redisKey, e);

            /*
             * 缓存内容异常时，删除脏缓存。
             */
            try {
                stringRedisTemplate.delete(redisKey);
            } catch (Exception deleteException) {
                log.warn("删除异常告警规则缓存失败，key={}", redisKey, deleteException);
            }
        }

        /*
         * 2. Redis 未命中，查询 MySQL
         */
        List<AlarmRule> rules = alarmRuleMapper.selectList(
                new LambdaQueryWrapper<AlarmRule>()
                        .eq(AlarmRule::getEnabled, RULE_ENABLED)
                        .eq(AlarmRule::getAlarmType, ALARM_TYPE_THRESHOLD)
                        .orderByAsc(AlarmRule::getId)
        );

        if (rules == null) {
            rules = Collections.emptyList();
        }

        /*
         * 3. 写入 Redis
         *
         * 注意：
         * 即使 rules 为空，也写入 Redis。
         * 这样可以防止在没有规则时，每条设备数据都打到数据库。
         */
        try {
            String cacheValue = objectMapper.writeValueAsString(rules);

            stringRedisTemplate.opsForValue()
                    .set(redisKey, cacheValue, CACHE_TTL);

            log.info("写入 Redis 告警规则缓存成功，key={}，ruleCount={}", redisKey, rules.size());
        } catch (Exception e) {
            log.warn("写入 Redis 告警规则缓存失败，key={}", redisKey, e);
        }

        return rules;
    }

    @Override
    public void clearThresholdRuleCache() {
        String redisKey = RedisKeyConstants.enabledAlarmRules(ALARM_TYPE_THRESHOLD);

        try {
            Boolean deleted = stringRedisTemplate.delete(redisKey);

            log.info("清理 Redis 告警规则缓存，key={}，deleted={}", redisKey, deleted);
        } catch (Exception e) {
            log.warn("清理 Redis 告警规则缓存失败，key={}", redisKey, e);
        }
    }
}
