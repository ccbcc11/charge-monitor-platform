package com.ccbcc.charge.monitor.module.workorder.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ccbcc.charge.monitor.module.alarm.entity.AlarmRecord;
import com.ccbcc.charge.monitor.module.alarm.mapper.AlarmRecordMapper;
import com.ccbcc.charge.monitor.module.workorder.entity.WorkOrder;
import com.ccbcc.charge.monitor.module.workorder.mapper.WorkOrderMapper;
import com.ccbcc.charge.monitor.module.workorder.service.WorkOrderAutoGenerateService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 自动工单生成服务实现类
 *
 * 第一版规则：
 * 严重告警 alarm_level = 3 自动生成工单。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WorkOrderAutoGenerateServiceImpl implements WorkOrderAutoGenerateService {

    /**
     * 告警等级：严重
     */
    private static final Integer ALARM_LEVEL_SERIOUS = 3;

    /**
     * 告警状态：未确认
     */
    private static final Integer ALARM_STATUS_UNCONFIRMED = 0;

    /**
     * 告警状态：已确认
     */
    private static final Integer ALARM_STATUS_CONFIRMED = 1;

    /**
     * 已生成工单
     */
    private static final Integer WORK_ORDER_GENERATED = 1;

    /**
     * 工单优先级：高
     */
    private static final Integer PRIORITY_HIGH = 3;

    /**
     * 工单状态：待处理
     */
    private static final Integer STATUS_PENDING = 0;

    private final AlarmRecordMapper alarmRecordMapper;

    private final WorkOrderMapper workOrderMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void generateForAlarmIds(List<Long> alarmIds) {
        if (CollectionUtils.isEmpty(alarmIds)) {
            return;
        }

        List<AlarmRecord> alarmRecords = alarmRecordMapper.selectBatchIds(alarmIds);

        if (CollectionUtils.isEmpty(alarmRecords)) {
            log.warn("自动工单生成跳过：未查询到告警记录，alarmIds={}", alarmIds);
            return;
        }

        for (AlarmRecord alarmRecord : alarmRecords) {
            generateForSingleAlarm(alarmRecord);
        }
    }

    /**
     * 为单个告警生成工单
     */
    private void generateForSingleAlarm(AlarmRecord alarmRecord) {
        if (alarmRecord == null) {
            return;
        }

        /*
         * 1. 只处理严重告警
         */
        if (!ALARM_LEVEL_SERIOUS.equals(alarmRecord.getAlarmLevel())) {
            log.debug(
                    "自动工单跳过：非严重告警，alarmId={}，alarmLevel={}",
                    alarmRecord.getId(),
                    alarmRecord.getAlarmLevel()
            );
            return;
        }

        /*
         * 2. 只处理未恢复告警
         */
        if (!ALARM_STATUS_UNCONFIRMED.equals(alarmRecord.getAlarmStatus())
                && !ALARM_STATUS_CONFIRMED.equals(alarmRecord.getAlarmStatus())) {
            log.debug(
                    "自动工单跳过：告警已恢复或状态不符合，alarmId={}，alarmStatus={}",
                    alarmRecord.getId(),
                    alarmRecord.getAlarmStatus()
            );
            return;
        }

        /*
         * 3. 已生成工单则跳过
         */
        if (WORK_ORDER_GENERATED.equals(alarmRecord.getWorkOrderGenerated())) {
            log.debug("自动工单跳过：告警已生成工单，alarmId={}", alarmRecord.getId());
            return;
        }

        /*
         * 4. 再查一次工单表，避免 alarm_record 标记异常导致重复生成
         */
        Long existingCount = workOrderMapper.selectCount(
                new LambdaQueryWrapper<WorkOrder>()
                        .eq(WorkOrder::getAlarmId, alarmRecord.getId())
        );

        if (existingCount != null && existingCount > 0) {
            markAlarmWorkOrderGenerated(alarmRecord.getId());
            log.debug("自动工单跳过：工单已存在，alarmId={}", alarmRecord.getId());
            return;
        }

        /*
         * 5. 创建工单
         */
        WorkOrder workOrder = buildWorkOrder(alarmRecord);

        try {
            workOrderMapper.insert(workOrder);

            /*
             * 6. 回写 alarm_record.work_order_generated = 1
             */
            markAlarmWorkOrderGenerated(alarmRecord.getId());

            log.info(
                    "严重告警自动生成工单成功，alarmId={}，workOrderId={}，workOrderNo={}，deviceCode={}",
                    alarmRecord.getId(),
                    workOrder.getId(),
                    workOrder.getWorkOrderNo(),
                    alarmRecord.getDeviceCode()
            );
        } catch (DuplicateKeyException e) {
            /*
             * 并发情况下，可能两个线程同时试图为同一个 alarm_id 创建工单。
             * 数据库唯一索引 uk_alarm_id 会拦住第二次插入。
             * 这里捕获后回写告警标记即可。
             */
            markAlarmWorkOrderGenerated(alarmRecord.getId());

            log.warn(
                    "严重告警自动生成工单重复，已忽略，alarmId={}，deviceCode={}",
                    alarmRecord.getId(),
                    alarmRecord.getDeviceCode()
            );
        }
    }

    /**
     * 构建工单对象
     */
    private WorkOrder buildWorkOrder(AlarmRecord alarmRecord) {
        LocalDateTime now = LocalDateTime.now();

        return new WorkOrder()
                .setWorkOrderNo(generateWorkOrderNo())
                .setAlarmId(alarmRecord.getId())
                .setDeviceId(alarmRecord.getDeviceId())
                .setDeviceCode(alarmRecord.getDeviceCode())
                .setTitle(buildWorkOrderTitle(alarmRecord))
                .setContent(buildWorkOrderContent(alarmRecord))
                .setPriority(PRIORITY_HIGH)
                .setStatus(STATUS_PENDING)
                .setAssigneeId(null)
                .setAssigneeName(null)
                .setFinishTime(null)
                .setRemark("系统根据严重告警自动生成工单")
                .setCreateTime(now)
                .setUpdateTime(now)
                .setDeleted(0);
    }

    /**
     * 构建工单标题
     */
    private String buildWorkOrderTitle(AlarmRecord alarmRecord) {
        return "【严重告警】设备 " + alarmRecord.getDeviceCode()
                + " 发生 " + alarmRecord.getAlarmMetric() + " 异常";
    }

    /**
     * 构建工单内容
     */
    private String buildWorkOrderContent(AlarmRecord alarmRecord) {
        return "设备编号：" + alarmRecord.getDeviceCode()
                + "\n告警编号：" + alarmRecord.getAlarmNo()
                + "\n告警类型：" + alarmRecord.getAlarmType()
                + "\n告警指标：" + alarmRecord.getAlarmMetric()
                + "\n告警等级：" + alarmRecord.getAlarmLevel()
                + "\n当前值：" + alarmRecord.getCurrentValue()
                + "\n阈值：" + alarmRecord.getThresholdValue()
                + "\n告警内容：" + alarmRecord.getAlarmMessage()
                + "\n首次告警时间：" + alarmRecord.getFirstTime()
                + "\n最近告警时间：" + alarmRecord.getLastTime();
    }

    /**
     * 标记告警已生成工单
     */
    private void markAlarmWorkOrderGenerated(Long alarmId) {
        if (alarmId == null) {
            return;
        }

        AlarmRecord update = new AlarmRecord()
                .setId(alarmId)
                .setWorkOrderGenerated(1)
                .setUpdateTime(LocalDateTime.now());

        alarmRecordMapper.updateById(update);
    }

    /**
     * 生成工单编号
     */
    private String generateWorkOrderNo() {
        String timePart = LocalDateTime.now()
                .format(DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS"));

        int randomPart = ThreadLocalRandom.current().nextInt(1000, 10000);

        return "WO" + timePart + randomPart;
    }
}
