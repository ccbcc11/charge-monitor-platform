package com.ccbcc.charge.monitor.module.alarm.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ccbcc.charge.monitor.module.alarm.entity.AlarmRule;
import org.apache.ibatis.annotations.Mapper;

/**
 * 告警规则 Mapper
 */
@Mapper
public interface AlarmRuleMapper extends BaseMapper<AlarmRule> {
}