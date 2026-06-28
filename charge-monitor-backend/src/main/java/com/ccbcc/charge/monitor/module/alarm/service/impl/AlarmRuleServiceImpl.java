package com.ccbcc.charge.monitor.module.alarm.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ccbcc.charge.monitor.common.exception.BusinessException;
import com.ccbcc.charge.monitor.common.result.PageResult;
import com.ccbcc.charge.monitor.common.result.ResultCode;
import com.ccbcc.charge.monitor.module.alarm.dto.AlarmRuleCreateDTO;
import com.ccbcc.charge.monitor.module.alarm.dto.AlarmRulePageQueryDTO;
import com.ccbcc.charge.monitor.module.alarm.dto.AlarmRuleUpdateDTO;
import com.ccbcc.charge.monitor.module.alarm.entity.AlarmRule;
import com.ccbcc.charge.monitor.module.alarm.mapper.AlarmRuleMapper;
import com.ccbcc.charge.monitor.module.alarm.service.AlarmRuleCacheService;
import com.ccbcc.charge.monitor.module.alarm.service.AlarmRuleService;
import com.ccbcc.charge.monitor.module.alarm.vo.AlarmRuleDetailVO;
import com.ccbcc.charge.monitor.module.alarm.vo.AlarmRulePageVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 告警规则管理服务实现类
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AlarmRuleServiceImpl implements AlarmRuleService {

    private final AlarmRuleMapper alarmRuleMapper;
    private final AlarmRuleCacheService alarmRuleCacheService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createAlarmRule(AlarmRuleCreateDTO createDTO) {

        /*
         * 1. 判断规则编码是否已存在
         */
        Long count = alarmRuleMapper.selectCount(
                new LambdaQueryWrapper<AlarmRule>()
                        .eq(AlarmRule::getRuleCode, createDTO.getRuleCode())
        );

        if (count != null && count > 0) {
            throw new BusinessException(ResultCode.ALARM_RULE_CODE_EXISTS, "规则编码已存在：" + createDTO.getRuleCode());
        }

        /*
         * 2. DTO 转 Entity
         */
        AlarmRule rule = new AlarmRule();
        BeanUtils.copyProperties(createDTO, rule);

        LocalDateTime now = LocalDateTime.now();
        rule.setCreateTime(now);
        rule.setUpdateTime(now);
        rule.setDeleted(0);

        /*
         * 3. 写入数据库
         */
        alarmRuleMapper.insert(rule);

        alarmRuleCacheService.clearThresholdRuleCache();

        log.info("新增告警规则，ruleCode={}，ruleName={}，alarmType={}，metricName={}",
                rule.getRuleCode(), rule.getRuleName(), rule.getAlarmType(), rule.getMetricName());

        return rule.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateAlarmRule(Long id, AlarmRuleUpdateDTO updateDTO) {

        /*
         * 1. 查询规则是否存在
         */
        AlarmRule existing = alarmRuleMapper.selectById(id);

        if (existing == null) {
            throw new BusinessException(ResultCode.ALARM_RULE_NOT_FOUND);
        }

        /*
         * 2. 更新允许修改的字段
         *
         * 不允许通过该接口修改：
         * ruleCode（规则编码创建后不可修改，保证告警记录的规则引用一致性）
         */
        AlarmRule rule = new AlarmRule();
        BeanUtils.copyProperties(updateDTO, rule);

        rule.setId(id);
        rule.setUpdateTime(LocalDateTime.now());

        alarmRuleMapper.updateById(rule);

        alarmRuleCacheService.clearThresholdRuleCache();

        log.info("修改告警规则，id={}，ruleCode={}，ruleName={}",
                id, existing.getRuleCode(), updateDTO.getRuleName());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteAlarmRule(Long id) {

        /*
         * 1. 查询规则是否存在
         */
        AlarmRule existing = alarmRuleMapper.selectById(id);

        if (existing == null) {
            throw new BusinessException(ResultCode.ALARM_RULE_NOT_FOUND);
        }

        /*
         * 2. 逻辑删除
         *
         * AlarmRule 中 deleted 字段已标注 @TableLogic，
         * MyBatis-Plus 会把 deleteById 转成 UPDATE deleted = 1。
         */
        alarmRuleMapper.deleteById(id);

        alarmRuleCacheService.clearThresholdRuleCache();

        log.info("删除告警规则，id={}，ruleCode={}，ruleName={}",
                id, existing.getRuleCode(), existing.getRuleName());
    }

    @Override
    public PageResult<AlarmRulePageVO> pageAlarmRule(AlarmRulePageQueryDTO queryDTO) {

        Page<AlarmRule> page = new Page<>(queryDTO.getPageNo(), queryDTO.getPageSize());

        LambdaQueryWrapper<AlarmRule> wrapper = new LambdaQueryWrapper<AlarmRule>()
                .like(StringUtils.hasText(queryDTO.getRuleCode()),
                        AlarmRule::getRuleCode,
                        queryDTO.getRuleCode())
                .like(StringUtils.hasText(queryDTO.getRuleName()),
                        AlarmRule::getRuleName,
                        queryDTO.getRuleName())
                .eq(StringUtils.hasText(queryDTO.getAlarmType()),
                        AlarmRule::getAlarmType,
                        queryDTO.getAlarmType())
                .eq(StringUtils.hasText(queryDTO.getDeviceType()),
                        AlarmRule::getDeviceType,
                        queryDTO.getDeviceType())
                .eq(StringUtils.hasText(queryDTO.getMetricName()),
                        AlarmRule::getMetricName,
                        queryDTO.getMetricName())
                .eq(queryDTO.getEnabled() != null,
                        AlarmRule::getEnabled,
                        queryDTO.getEnabled())
                .eq(queryDTO.getAlarmLevel() != null,
                        AlarmRule::getAlarmLevel,
                        queryDTO.getAlarmLevel())
                .orderByAsc(AlarmRule::getAlarmType)
                .orderByAsc(AlarmRule::getId);

        Page<AlarmRule> resultPage = alarmRuleMapper.selectPage(page, wrapper);

        List<AlarmRulePageVO> records = resultPage.getRecords()
                .stream()
                .map(this::convertToPageVO)
                .toList();

        return new PageResult<>(
                records,
                resultPage.getTotal(),
                resultPage.getCurrent(),
                resultPage.getSize(),
                resultPage.getPages()
        );
    }

    @Override
    public AlarmRuleDetailVO getAlarmRuleDetail(Long id) {

        AlarmRule rule = alarmRuleMapper.selectById(id);

        if (rule == null) {
            throw new BusinessException(ResultCode.ALARM_RULE_NOT_FOUND);
        }

        return convertToDetailVO(rule);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void enableAlarmRule(Long id) {

        AlarmRule existing = alarmRuleMapper.selectById(id);

        if (existing == null) {
            throw new BusinessException(ResultCode.ALARM_RULE_NOT_FOUND);
        }

        if (existing.getEnabled() != null && existing.getEnabled() == 1) {
            log.info("告警规则已是启用状态，无需重复操作，id={}，ruleCode={}", id, existing.getRuleCode());
            return;
        }

        AlarmRule rule = new AlarmRule();
        rule.setId(id);
        rule.setEnabled(1);
        rule.setUpdateTime(LocalDateTime.now());

        alarmRuleMapper.updateById(rule);

        alarmRuleCacheService.clearThresholdRuleCache();

        log.info("启用告警规则，id={}，ruleCode={}，ruleName={}",
                id, existing.getRuleCode(), existing.getRuleName());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void disableAlarmRule(Long id) {

        AlarmRule existing = alarmRuleMapper.selectById(id);

        if (existing == null) {
            throw new BusinessException(ResultCode.ALARM_RULE_NOT_FOUND);
        }

        if (existing.getEnabled() != null && existing.getEnabled() == 0) {
            log.info("告警规则已是禁用状态，无需重复操作，id={}，ruleCode={}", id, existing.getRuleCode());
            return;
        }

        AlarmRule rule = new AlarmRule();
        rule.setId(id);
        rule.setEnabled(0);
        rule.setUpdateTime(LocalDateTime.now());

        alarmRuleMapper.updateById(rule);

        alarmRuleCacheService.clearThresholdRuleCache();

        log.info("禁用告警规则，id={}，ruleCode={}，ruleName={}",
                id, existing.getRuleCode(), existing.getRuleName());
    }

    /**
     * Entity 转分页 VO
     */
    private AlarmRulePageVO convertToPageVO(AlarmRule rule) {
        AlarmRulePageVO vo = new AlarmRulePageVO();
        BeanUtils.copyProperties(rule, vo);
        return vo;
    }

    /**
     * Entity 转详情 VO
     */
    private AlarmRuleDetailVO convertToDetailVO(AlarmRule rule) {
        AlarmRuleDetailVO vo = new AlarmRuleDetailVO();
        BeanUtils.copyProperties(rule, vo);
        return vo;
    }
}
