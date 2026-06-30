package com.ccbcc.charge.monitor.module.workorder.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 工单分页查询参数
 */
@Data
@Schema(description = "工单分页查询参数")
public class WorkOrderPageQueryDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Min(value = 1, message = "页码不能小于1")
    @Schema(description = "页码，默认1", example = "1")
    private Long pageNo = 1L;

    @Min(value = 1, message = "每页数量不能小于1")
    @Max(value = 100, message = "每页数量不能超过100")
    @Schema(description = "每页数量，默认10，最大100", example = "10")
    private Long pageSize = 10L;

    @Schema(description = "工单编号，模糊查询", example = "WO202606")
    private String workOrderNo;

    @Schema(description = "设备编号，模糊查询", example = "CP-0001")
    private String deviceCode;

    @Min(value = 0, message = "工单状态只能为0、1、2、3")
    @Max(value = 3, message = "工单状态只能为0、1、2、3")
    @Schema(description = "工单状态：0待处理，1处理中，2已完成，3已关闭", example = "0")
    private Integer status;

    @Min(value = 1, message = "优先级只能为1、2、3")
    @Max(value = 3, message = "优先级只能为1、2、3")
    @Schema(description = "优先级：1低，2中，3高", example = "3")
    private Integer priority;
}
