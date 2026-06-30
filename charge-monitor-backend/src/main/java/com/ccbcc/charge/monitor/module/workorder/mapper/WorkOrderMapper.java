package com.ccbcc.charge.monitor.module.workorder.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ccbcc.charge.monitor.module.workorder.entity.WorkOrder;
import org.apache.ibatis.annotations.Mapper;

/**
 * 运维工单 Mapper
 */
@Mapper
public interface WorkOrderMapper extends BaseMapper<WorkOrder> {
}
