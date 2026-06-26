package com.ccbcc.charge.monitor.module.device.controller;

import com.ccbcc.charge.monitor.common.result.PageResult;
import com.ccbcc.charge.monitor.common.result.Result;
import com.ccbcc.charge.monitor.module.device.dto.DeviceCreateDTO;
import com.ccbcc.charge.monitor.module.device.dto.DevicePageQueryDTO;
import com.ccbcc.charge.monitor.module.device.dto.DeviceUpdateDTO;
import com.ccbcc.charge.monitor.module.device.service.DeviceService;
import com.ccbcc.charge.monitor.module.device.vo.DeviceDetailVO;
import com.ccbcc.charge.monitor.module.device.vo.DevicePageVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * 设备管理接口
 */
@Validated
@Tag(name = "设备模块")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/device")
public class DeviceController {

    private final DeviceService deviceService;

    /**
     * 新增设备
     */
    @Operation(summary = "新增设备")
    @PostMapping
    public Result<Long> createDevice(@Valid @RequestBody DeviceCreateDTO createDTO) {
        return Result.success(deviceService.createDevice(createDTO));
    }

    /**
     * 修改设备
     */
    @Operation(summary = "修改设备")
    @PutMapping("/{id}")
    public Result<Void> updateDevice(@PathVariable Long id,
                                     @Valid @RequestBody DeviceUpdateDTO updateDTO) {
        deviceService.updateDevice(id, updateDTO);
        return Result.success();
    }

    /**
     * 删除设备
     */
    @Operation(summary = "删除设备")
    @DeleteMapping("/{id}")
    public Result<Void> deleteDevice(@PathVariable Long id) {
        deviceService.deleteDevice(id);
        return Result.success();
    }

    /**
     * 分页查询设备
     */
    @Operation(summary = "分页查询设备")
    @GetMapping("/page")
    public Result<PageResult<DevicePageVO>> pageDevice(
            @Valid @ParameterObject DevicePageQueryDTO queryDTO
    ) {
        return Result.success(deviceService.pageDevice(queryDTO));
    }

    /**
     * 查询设备详情
     */
    @Operation(summary = "查询设备详情")
    @GetMapping("/{id}")
    public Result<DeviceDetailVO> getDeviceDetail(@PathVariable Long id) {
        return Result.success(deviceService.getDeviceDetail(id));
    }
}