package com.ccbcc.charge.monitor.module.workorder.service;

import java.util.List;

/**
 * 自动工单生成服务
 */
public interface WorkOrderAutoGenerateService {

    /**
     * 根据告警 ID 列表自动生成工单
     *
     * 当前规则：
     * 1. 只处理 alarm_level = 3 的严重告警
     * 2. 只处理未恢复告警
     * 3. 已生成工单的告警不重复生成
     */
    void generateForAlarmIds(List<Long> alarmIds);
}
