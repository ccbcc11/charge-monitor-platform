package com.ccbcc.charge.monitor.module.alarm.controller;

import com.ccbcc.charge.monitor.common.result.PageResult;
import com.ccbcc.charge.monitor.common.result.Result;
import com.ccbcc.charge.monitor.module.alarm.dto.AlarmRuleCreateDTO;
import com.ccbcc.charge.monitor.module.alarm.dto.AlarmRulePageQueryDTO;
import com.ccbcc.charge.monitor.module.alarm.dto.AlarmRuleUpdateDTO;
import com.ccbcc.charge.monitor.module.alarm.service.AlarmRuleService;
import com.ccbcc.charge.monitor.module.alarm.vo.AlarmRuleDetailVO;
import com.ccbcc.charge.monitor.module.alarm.vo.AlarmRulePageVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * 告警规则管理接口
 */
@Validated
@Tag(name = "告警规则模块")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/alarm/rule")
public class AlarmRuleController {

    private final AlarmRuleService alarmRuleService;

    /**
     * 新增告警规则
     */
    @Operation(summary = "新增告警规则")
    @PostMapping
    public Result<Long> createAlarmRule(@Valid @RequestBody AlarmRuleCreateDTO createDTO) {
        return Result.success(alarmRuleService.createAlarmRule(createDTO));
    }

    /**
     * 修改告警规则
     */
    @Operation(summary = "修改告警规则")
    @PutMapping("/{id}")
    public Result<Void> updateAlarmRule(@PathVariable Long id,
                                         @Valid @RequestBody AlarmRuleUpdateDTO updateDTO) {
        alarmRuleService.updateAlarmRule(id, updateDTO);
        return Result.success();
    }

    /**
     * 删除告警规则
     */
    @Operation(summary = "删除告警规则")
    @DeleteMapping("/{id}")
    public Result<Void> deleteAlarmRule(@PathVariable Long id) {
        alarmRuleService.deleteAlarmRule(id);
        return Result.success();
    }

    /**
     * 分页查询告警规则
     */
    @Operation(summary = "分页查询告警规则")
    @GetMapping("/page")
    public Result<PageResult<AlarmRulePageVO>> pageAlarmRule(
            @Valid @ParameterObject AlarmRulePageQueryDTO queryDTO
    ) {
        return Result.success(alarmRuleService.pageAlarmRule(queryDTO));
    }

    /**
     * 查询告警规则详情
     */
    @Operation(summary = "查询告警规则详情")
    @GetMapping("/{id}")
    public Result<AlarmRuleDetailVO> getAlarmRuleDetail(@PathVariable Long id) {
        return Result.success(alarmRuleService.getAlarmRuleDetail(id));
    }

    /**
     * 启用告警规则
     */
    @Operation(summary = "启用告警规则")
    @PutMapping("/{id}/enable")
    public Result<Void> enableAlarmRule(@PathVariable Long id) {
        alarmRuleService.enableAlarmRule(id);
        return Result.success();
    }

    /**
     * 禁用告警规则
     */
    @Operation(summary = "禁用告警规则")
    @PutMapping("/{id}/disable")
    public Result<Void> disableAlarmRule(@PathVariable Long id) {
        alarmRuleService.disableAlarmRule(id);
        return Result.success();
    }
}
