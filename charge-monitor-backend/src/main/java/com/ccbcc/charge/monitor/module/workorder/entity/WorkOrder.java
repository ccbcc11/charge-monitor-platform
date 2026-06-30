package com.ccbcc.charge.monitor.module.workorder.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 运维工单实体
 */
@Data
@Accessors(chain = true)
@TableName("work_order")
public class WorkOrder implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 工单编号
     */
    private String workOrderNo;

    /**
     * 关联告警ID
     */
    private Long alarmId;

    /**
     * 设备ID
     */
    private Long deviceId;

    /**
     * 设备编号
     */
    private String deviceCode;

    /**
     * 工单标题
     */
    private String title;

    /**
     * 工单内容
     */
    private String content;

    /**
     * 优先级：1低，2中，3高
     */
    private Integer priority;

    /**
     * 工单状态：0待处理，1处理中，2已完成，3已关闭
     */
    private Integer status;

    /**
     * 处理人ID
     */
    private Long assigneeId;

    /**
     * 处理人名称
     */
    private String assigneeName;

    /**
     * 完成时间
     */
    private LocalDateTime finishTime;

    /**
     * 备注
     */
    private String remark;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

    @TableLogic
    private Integer deleted;
}
