package com.ccbcc.charge.monitor.module.alarm.service;

import com.ccbcc.charge.monitor.module.alarm.entity.AlarmRule;

import java.util.List;

/**
 * 告警规则缓存服务
 */
public interface AlarmRuleCacheService {

    /**
     * 获取启用的阈值告警规则
     *
     * 优先从 Redis 获取；
     * Redis 未命中时查询 MySQL，并回写 Redis。
     */
    List<AlarmRule> getEnabledThresholdRules();

    /**
     * 清理阈值告警规则缓存
     *
     * 规则新增、修改、删除、启用、禁用后调用。
     */
    void clearThresholdRuleCache();
}
