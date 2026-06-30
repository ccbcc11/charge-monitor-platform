package com.ccbcc.charge.monitor.module.workorder.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 工单详情返回
 */
@Data
@Accessors(chain = true)
@Schema(description = "工单详情返回")
public class WorkOrderDetailVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "工单ID", example = "1")
    private Long id;

    @Schema(description = "工单编号", example = "WO202606301100001234")
    private String workOrderNo;

    @Schema(description = "关联告警ID", example = "17")
    private Long alarmId;

    @Schema(description = "设备ID", example = "1")
    private Long deviceId;

    @Schema(description = "设备编号", example = "CP-0001")
    private String deviceCode;

    @Schema(description = "工单标题", example = "【严重告警】设备 CP-0001 发生 temperature 异常")
    private String title;

    @Schema(description = "工单内容")
    private String content;

    @Schema(description = "优先级：1低，2中，3高", example = "3")
    private Integer priority;

    @Schema(description = "工单状态：0待处理，1处理中，2已完成，3已关闭", example = "0")
    private Integer status;

    @Schema(description = "处理人ID", example = "2")
    private Long assigneeId;

    @Schema(description = "处理人名称", example = "张三")
    private String assigneeName;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "完成时间", example = "2026-06-30 11:30:00")
    private LocalDateTime finishTime;

    @Schema(description = "备注")
    private String remark;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "创建时间", example = "2026-06-30 11:00:00")
    private LocalDateTime createTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "更新时间", example = "2026-06-30 11:30:00")
    private LocalDateTime updateTime;
}
