package com.ccbcc.charge.monitor.module.device.controller;

import com.ccbcc.charge.monitor.common.result.PageResult;
import com.ccbcc.charge.monitor.common.result.Result;
import com.ccbcc.charge.monitor.module.device.dto.DeviceDataHistoryQueryDTO;
import com.ccbcc.charge.monitor.module.device.dto.DeviceDataReportDTO;
import com.ccbcc.charge.monitor.module.device.service.DeviceDataService;
import com.ccbcc.charge.monitor.module.device.vo.DeviceDataHistoryVO;
import com.ccbcc.charge.monitor.module.device.vo.DeviceDataReportVO;
import com.ccbcc.charge.monitor.module.device.vo.DeviceLatestStatusVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * 设备运行数据接口
 */
@Validated
@Tag(name = "设备数据模块")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/device/data")
public class DeviceDataController {

    private final DeviceDataService deviceDataService;

    /**
     * 上报设备运行数据
     */
    @Operation(summary = "上报设备运行数据")
    @PostMapping("/report")
    public Result<DeviceDataReportVO> report(@Valid @RequestBody DeviceDataReportDTO reportDTO) {
        return Result.success(deviceDataService.report(reportDTO));
    }

    /**
     * 查询设备最新状态
     */
    @Operation(summary = "查询设备最新状态")
    @GetMapping("/latest/{deviceCode}")
    public Result<DeviceLatestStatusVO> getLatestStatus(@PathVariable String deviceCode) {
        return Result.success(deviceDataService.getLatestStatus(deviceCode));
    }

    /**
     * 查询设备历史运行数据
     */
    @Operation(summary = "查询设备历史运行数据")
    @GetMapping("/history")
    public Result<PageResult<DeviceDataHistoryVO>> pageHistory(
            @Valid @ParameterObject DeviceDataHistoryQueryDTO queryDTO
    ) {
        return Result.success(deviceDataService.pageHistory(queryDTO));
    }
}