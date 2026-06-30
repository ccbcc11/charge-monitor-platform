package com.ccbcc.charge.monitor.module.workorder.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 工单关闭请求参数
 */
@Data
@Schema(description = "工单关闭请求参数")
public class WorkOrderCloseDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Size(max = 255, message = "备注长度不能超过255个字符")
    @Schema(description = "关闭备注", example = "误报，关闭工单")
    private String remark;
}
