package com.ccbcc.charge.monitor.module.device.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

/**
 * 设备运行数据上报响应结果
 */
@Data
@Accessors(chain = true)
@Schema(description = "设备运行数据上报响应结果")
public class DeviceDataReportVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 运行数据ID
     */
    @Schema(description = "运行数据ID", example = "1001")
    private Long dataId;

    /**
     * 设备编号
     */
    @Schema(description = "设备编号", example = "CP-0001")
    private String deviceCode;

    /**
     * 是否触发告警
     */
    @Schema(description = "是否触发告警", example = "true")
    private Boolean alarmTriggered;

    /**
     * 本次触发或更新的告警ID列表
     */
    @Schema(description = "本次触发或更新的告警ID列表", example = "[2001]")
    private List<Long> alarmIds;
}