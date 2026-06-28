package com.ccbcc.charge.monitor.module.alarm.service;

import com.ccbcc.charge.monitor.common.result.PageResult;
import com.ccbcc.charge.monitor.module.alarm.dto.AlarmRuleCreateDTO;
import com.ccbcc.charge.monitor.module.alarm.dto.AlarmRulePageQueryDTO;
import com.ccbcc.charge.monitor.module.alarm.dto.AlarmRuleUpdateDTO;
import com.ccbcc.charge.monitor.module.alarm.vo.AlarmRuleDetailVO;
import com.ccbcc.charge.monitor.module.alarm.vo.AlarmRulePageVO;

/**
 * 告警规则管理服务接口
 */
public interface AlarmRuleService {

    /**
     * 新增告警规则
     *
     * @param createDTO 新增告警规则请求参数
     * @return 规则ID
     */
    Long createAlarmRule(AlarmRuleCreateDTO createDTO);

    /**
     * 修改告警规则
     *
     * @param id        规则ID
     * @param updateDTO 修改告警规则请求参数
     */
    void updateAlarmRule(Long id, AlarmRuleUpdateDTO updateDTO);

    /**
     * 删除告警规则
     *
     * @param id 规则ID
     */
    void deleteAlarmRule(Long id);

    /**
     * 分页查询告警规则
     *
     * @param queryDTO 查询参数
     * @return 分页结果
     */
    PageResult<AlarmRulePageVO> pageAlarmRule(AlarmRulePageQueryDTO queryDTO);

    /**
     * 查询告警规则详情
     *
     * @param id 规则ID
     * @return 规则详情
     */
    AlarmRuleDetailVO getAlarmRuleDetail(Long id);

    /**
     * 启用告警规则
     *
     * @param id 规则ID
     */
    void enableAlarmRule(Long id);

    /**
     * 禁用告警规则
     *
     * @param id 规则ID
     */
    void disableAlarmRule(Long id);
}
