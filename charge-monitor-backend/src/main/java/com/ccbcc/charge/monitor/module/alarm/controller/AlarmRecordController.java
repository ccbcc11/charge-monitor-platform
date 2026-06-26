package com.ccbcc.charge.monitor.module.alarm.controller;

import com.ccbcc.charge.monitor.common.result.PageResult;
import com.ccbcc.charge.monitor.common.result.Result;
import com.ccbcc.charge.monitor.module.alarm.dto.AlarmRecordPageQueryDTO;
import com.ccbcc.charge.monitor.module.alarm.service.AlarmRecordService;
import com.ccbcc.charge.monitor.module.alarm.vo.AlarmRecordDetailVO;
import com.ccbcc.charge.monitor.module.alarm.vo.AlarmRecordPageVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * 告警记录接口
 */
@Validated
@Tag(name = "告警模块")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/alarm/record")
public class AlarmRecordController {

    private final AlarmRecordService alarmRecordService;

    /**
     * 分页查询告警记录
     */
    @Operation(summary = "分页查询告警记录")
    @GetMapping("/page")
    public Result<PageResult<AlarmRecordPageVO>> pageAlarmRecord(
            @Valid @ParameterObject AlarmRecordPageQueryDTO queryDTO
    ) {
        return Result.success(alarmRecordService.pageAlarmRecord(queryDTO));
    }

    /**
     * 查询告警详情
     */
    @Operation(summary = "查询告警详情")
    @GetMapping("/{id}")
    public Result<AlarmRecordDetailVO> getAlarmRecordDetail(@PathVariable Long id) {
        return Result.success(alarmRecordService.getAlarmRecordDetail(id));
    }

    /**
     * 确认告警
     */
    @Operation(summary = "确认告警")
    @PutMapping("/{id}/ack")
    public Result<Void> ackAlarmRecord(@PathVariable Long id) {
        alarmRecordService.ackAlarmRecord(id);
        return Result.success();
    }

    /**
     * 恢复告警
     */
    @Operation(summary = "恢复告警")
    @PutMapping("/{id}/recover")
    public Result<Void> recoverAlarmRecord(@PathVariable Long id) {
        alarmRecordService.recoverAlarmRecord(id);
        return Result.success();
    }
}