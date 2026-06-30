package com.ccbcc.charge.monitor.module.workorder.controller;

import com.ccbcc.charge.monitor.common.result.PageResult;
import com.ccbcc.charge.monitor.common.result.Result;
import com.ccbcc.charge.monitor.module.workorder.dto.WorkOrderAcceptDTO;
import com.ccbcc.charge.monitor.module.workorder.dto.WorkOrderCloseDTO;
import com.ccbcc.charge.monitor.module.workorder.dto.WorkOrderFinishDTO;
import com.ccbcc.charge.monitor.module.workorder.dto.WorkOrderPageQueryDTO;
import com.ccbcc.charge.monitor.module.workorder.service.WorkOrderService;
import com.ccbcc.charge.monitor.module.workorder.vo.WorkOrderDetailVO;
import com.ccbcc.charge.monitor.module.workorder.vo.WorkOrderPageVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * 工单管理接口
 */
@Validated
@Tag(name = "工单模块")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/work-order")
public class WorkOrderController {

    private final WorkOrderService workOrderService;

    /**
     * 分页查询工单
     */
    @Operation(summary = "分页查询工单")
    @GetMapping("/page")
    public Result<PageResult<WorkOrderPageVO>> pageWorkOrder(
            @Valid @ParameterObject WorkOrderPageQueryDTO queryDTO) {
        return Result.success(workOrderService.pageWorkOrder(queryDTO));
    }

    /**
     * 查询工单详情
     */
    @Operation(summary = "查询工单详情")
    @GetMapping("/{id}")
    public Result<WorkOrderDetailVO> getWorkOrderDetail(@PathVariable Long id) {
        return Result.success(workOrderService.getWorkOrderDetail(id));
    }

    /**
     * 接单（开始处理）
     */
    @Operation(summary = "接单（开始处理）")
    @PutMapping("/{id}/accept")
    public Result<Void> acceptWorkOrder(@PathVariable Long id,
                                         @Valid @RequestBody WorkOrderAcceptDTO acceptDTO) {
        workOrderService.acceptWorkOrder(id, acceptDTO);
        return Result.success();
    }

    /**
     * 完成工单
     */
    @Operation(summary = "完成工单")
    @PutMapping("/{id}/finish")
    public Result<Void> finishWorkOrder(@PathVariable Long id,
                                         @Valid @RequestBody WorkOrderFinishDTO finishDTO) {
        workOrderService.finishWorkOrder(id, finishDTO);
        return Result.success();
    }

    /**
     * 关闭工单
     */
    @Operation(summary = "关闭工单")
    @PutMapping("/{id}/close")
    public Result<Void> closeWorkOrder(@PathVariable Long id,
                                        @Valid @RequestBody WorkOrderCloseDTO closeDTO) {
        workOrderService.closeWorkOrder(id, closeDTO);
        return Result.success();
    }

    /**
     * 删除工单
     */
    @Operation(summary = "删除工单")
    @DeleteMapping("/{id}")
    public Result<Void> deleteWorkOrder(@PathVariable Long id) {
        workOrderService.deleteWorkOrder(id);
        return Result.success();
    }
}
