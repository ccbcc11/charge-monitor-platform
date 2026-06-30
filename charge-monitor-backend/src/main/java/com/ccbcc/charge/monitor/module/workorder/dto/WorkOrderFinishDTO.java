package com.ccbcc.charge.monitor.module.workorder.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 工单完成请求参数
 */
@Data
@Schema(description = "工单完成请求参数")
public class WorkOrderFinishDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Size(max = 255, message = "备注长度不能超过255个字符")
    @Schema(description = "完成备注", example = "现场检查完成，设备温度恢复正常")
    private String remark;
}
