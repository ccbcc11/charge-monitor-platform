package com.ccbcc.charge.monitor.module.workorder.service;

import com.ccbcc.charge.monitor.common.result.PageResult;
import com.ccbcc.charge.monitor.module.workorder.dto.WorkOrderAcceptDTO;
import com.ccbcc.charge.monitor.module.workorder.dto.WorkOrderCloseDTO;
import com.ccbcc.charge.monitor.module.workorder.dto.WorkOrderFinishDTO;
import com.ccbcc.charge.monitor.module.workorder.dto.WorkOrderPageQueryDTO;
import com.ccbcc.charge.monitor.module.workorder.vo.WorkOrderDetailVO;
import com.ccbcc.charge.monitor.module.workorder.vo.WorkOrderPageVO;

/**
 * 工单管理服务接口
 */
public interface WorkOrderService {

    /**
     * 分页查询工单
     */
    PageResult<WorkOrderPageVO> pageWorkOrder(WorkOrderPageQueryDTO queryDTO);

    /**
     * 查询工单详情
     */
    WorkOrderDetailVO getWorkOrderDetail(Long id);

    /**
     * 接单（开始处理）
     * 0 待处理 → 1 处理中
     */
    void acceptWorkOrder(Long id, WorkOrderAcceptDTO acceptDTO);

    /**
     * 完成工单
     * 1 处理中 → 2 已完成
     */
    void finishWorkOrder(Long id, WorkOrderFinishDTO finishDTO);

    /**
     * 关闭工单
     * 0 待处理 / 1 处理中 → 3 已关闭
     */
    void closeWorkOrder(Long id, WorkOrderCloseDTO closeDTO);

    /**
     * 删除工单
     */
    void deleteWorkOrder(Long id);
}
