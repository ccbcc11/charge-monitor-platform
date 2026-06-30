package com.ccbcc.charge.monitor.module.workorder.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 工单接单请求参数
 */
@Data
@Schema(description = "工单接单请求参数")
public class WorkOrderAcceptDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "处理人ID", example = "2")
    private Long assigneeId;

    @Size(max = 100, message = "处理人名称长度不能超过100个字符")
    @Schema(description = "处理人名称", example = "张三")
    private String assigneeName;

    @Size(max = 255, message = "备注长度不能超过255个字符")
    @Schema(description = "备注", example = "已安排人员处理")
    private String remark;
}
