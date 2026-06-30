package com.ccbcc.charge.monitor.module.workorder.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ccbcc.charge.monitor.common.exception.BusinessException;
import com.ccbcc.charge.monitor.common.result.PageResult;
import com.ccbcc.charge.monitor.common.result.ResultCode;
import com.ccbcc.charge.monitor.module.workorder.dto.WorkOrderAcceptDTO;
import com.ccbcc.charge.monitor.module.workorder.dto.WorkOrderCloseDTO;
import com.ccbcc.charge.monitor.module.workorder.dto.WorkOrderFinishDTO;
import com.ccbcc.charge.monitor.module.workorder.dto.WorkOrderPageQueryDTO;
import com.ccbcc.charge.monitor.module.workorder.entity.WorkOrder;
import com.ccbcc.charge.monitor.module.workorder.mapper.WorkOrderMapper;
import com.ccbcc.charge.monitor.module.workorder.service.WorkOrderService;
import com.ccbcc.charge.monitor.module.workorder.vo.WorkOrderDetailVO;
import com.ccbcc.charge.monitor.module.workorder.vo.WorkOrderPageVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 工单管理服务实现类
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WorkOrderServiceImpl implements WorkOrderService {

    /**
     * 工单状态
     */
    private static final Integer STATUS_PENDING = 0;
    private static final Integer STATUS_PROCESSING = 1;
    private static final Integer STATUS_COMPLETED = 2;
    private static final Integer STATUS_CLOSED = 3;

    private final WorkOrderMapper workOrderMapper;

    @Override
    public PageResult<WorkOrderPageVO> pageWorkOrder(WorkOrderPageQueryDTO queryDTO) {

        Page<WorkOrder> page = new Page<>(queryDTO.getPageNo(), queryDTO.getPageSize());

        LambdaQueryWrapper<WorkOrder> wrapper = new LambdaQueryWrapper<WorkOrder>()
                .like(StringUtils.hasText(queryDTO.getWorkOrderNo()),
                        WorkOrder::getWorkOrderNo,
                        queryDTO.getWorkOrderNo())
                .like(StringUtils.hasText(queryDTO.getDeviceCode()),
                        WorkOrder::getDeviceCode,
                        queryDTO.getDeviceCode())
                .eq(queryDTO.getStatus() != null,
                        WorkOrder::getStatus,
                        queryDTO.getStatus())
                .eq(queryDTO.getPriority() != null,
                        WorkOrder::getPriority,
                        queryDTO.getPriority())
                .orderByDesc(WorkOrder::getCreateTime);

        Page<WorkOrder> resultPage = workOrderMapper.selectPage(page, wrapper);

        List<WorkOrderPageVO> records = resultPage.getRecords()
                .stream()
                .map(this::convertToPageVO)
                .toList();

        return new PageResult<>(
                records,
                resultPage.getTotal(),
                resultPage.getCurrent(),
                resultPage.getSize(),
                resultPage.getPages()
        );
    }

    @Override
    public WorkOrderDetailVO getWorkOrderDetail(Long id) {
        WorkOrder workOrder = getWorkOrderById(id);
        return convertToDetailVO(workOrder);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void acceptWorkOrder(Long id, WorkOrderAcceptDTO acceptDTO) {
        WorkOrder existing = getWorkOrderById(id);

        if (!STATUS_PENDING.equals(existing.getStatus())) {
            throw new BusinessException(ResultCode.WORK_ORDER_STATUS_ERROR,
                    "只有待处理状态的工单才能接单，当前状态：" + existing.getStatus());
        }

        WorkOrder update = new WorkOrder();
        update.setId(id);
        update.setStatus(STATUS_PROCESSING);
        update.setAssigneeId(acceptDTO.getAssigneeId());
        update.setAssigneeName(acceptDTO.getAssigneeName());
        update.setRemark(acceptDTO.getRemark());
        update.setUpdateTime(LocalDateTime.now());

        workOrderMapper.updateById(update);

        log.info("工单接单成功，id={}，workOrderNo={}，assigneeName={}",
                id, existing.getWorkOrderNo(), acceptDTO.getAssigneeName());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void finishWorkOrder(Long id, WorkOrderFinishDTO finishDTO) {
        WorkOrder existing = getWorkOrderById(id);

        if (!STATUS_PROCESSING.equals(existing.getStatus())) {
            throw new BusinessException(ResultCode.WORK_ORDER_STATUS_ERROR,
                    "只有处理中状态的工单才能完成，当前状态：" + existing.getStatus());
        }

        WorkOrder update = new WorkOrder();
        update.setId(id);
        update.setStatus(STATUS_COMPLETED);
        update.setFinishTime(LocalDateTime.now());
        update.setRemark(finishDTO.getRemark());
        update.setUpdateTime(LocalDateTime.now());

        workOrderMapper.updateById(update);

        log.info("工单完成，id={}，workOrderNo={}", id, existing.getWorkOrderNo());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void closeWorkOrder(Long id, WorkOrderCloseDTO closeDTO) {
        WorkOrder existing = getWorkOrderById(id);

        if (STATUS_COMPLETED.equals(existing.getStatus())) {
            throw new BusinessException(ResultCode.WORK_ORDER_STATUS_ERROR,
                    "已完成的工单不能关闭");
        }
        if (STATUS_CLOSED.equals(existing.getStatus())) {
            throw new BusinessException(ResultCode.WORK_ORDER_STATUS_ERROR,
                    "工单已关闭，不能重复关闭");
        }

        WorkOrder update = new WorkOrder();
        update.setId(id);
        update.setStatus(STATUS_CLOSED);
        update.setFinishTime(LocalDateTime.now());
        update.setRemark(closeDTO.getRemark());
        update.setUpdateTime(LocalDateTime.now());

        workOrderMapper.updateById(update);

        log.info("工单关闭，id={}，workOrderNo={}", id, existing.getWorkOrderNo());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteWorkOrder(Long id) {
        WorkOrder existing = getWorkOrderById(id);
        workOrderMapper.deleteById(id);

        log.info("工单删除，id={}，workOrderNo={}", id, existing.getWorkOrderNo());
    }

    private WorkOrder getWorkOrderById(Long id) {
        if (id == null) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "工单ID不能为空");
        }
        WorkOrder workOrder = workOrderMapper.selectById(id);
        if (workOrder == null) {
            throw new BusinessException(ResultCode.WORK_ORDER_NOT_FOUND);
        }
        return workOrder;
    }

    private WorkOrderPageVO convertToPageVO(WorkOrder workOrder) {
        WorkOrderPageVO vo = new WorkOrderPageVO();
        BeanUtils.copyProperties(workOrder, vo);
        return vo;
    }

    private WorkOrderDetailVO convertToDetailVO(WorkOrder workOrder) {
        WorkOrderDetailVO vo = new WorkOrderDetailVO();
        BeanUtils.copyProperties(workOrder, vo);
        return vo;
    }
}
