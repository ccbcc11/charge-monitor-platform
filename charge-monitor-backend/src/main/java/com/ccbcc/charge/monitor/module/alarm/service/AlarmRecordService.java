package com.ccbcc.charge.monitor.module.alarm.service;

import com.ccbcc.charge.monitor.common.result.PageResult;
import com.ccbcc.charge.monitor.module.alarm.dto.AlarmRecordPageQueryDTO;
import com.ccbcc.charge.monitor.module.alarm.vo.AlarmRecordDetailVO;
import com.ccbcc.charge.monitor.module.alarm.vo.AlarmRecordPageVO;

/**
 * 告警记录服务接口
 */
public interface AlarmRecordService {

    /**
     * 分页查询告警记录
     *
     * @param queryDTO 查询参数
     * @return 告警分页结果
     */
    PageResult<AlarmRecordPageVO> pageAlarmRecord(AlarmRecordPageQueryDTO queryDTO);

    /**
     * 查询告警详情
     *
     * @param id 告警ID
     * @return 告警详情
     */
    AlarmRecordDetailVO getAlarmRecordDetail(Long id);

    /**
     * 确认告警
     *
     * @param id 告警ID
     */
    void ackAlarmRecord(Long id);

    /**
     * 恢复告警
     *
     * @param id 告警ID
     */
    void recoverAlarmRecord(Long id);
}