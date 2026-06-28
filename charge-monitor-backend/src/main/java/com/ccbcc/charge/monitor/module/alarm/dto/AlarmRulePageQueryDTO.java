package com.ccbcc.charge.monitor.module.alarm.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 告警规则分页查询参数
 */
@Data
@Schema(description = "告警规则分页查询参数")
public class AlarmRulePageQueryDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 页码
     */
    @Min(value = 1, message = "页码不能小于1")
    @Schema(description = "页码，默认1", example = "1")
    private Long pageNo = 1L;

    /**
     * 每页数量
     */
    @Min(value = 1, message = "每页数量不能小于1")
    @Max(value = 100, message = "每页数量不能超过100")
    @Schema(description = "每页数量，默认10，最大100", example = "10")
    private Long pageSize = 10L;

    /**
     * 规则编码，模糊查询
     */
    @Schema(description = "规则编码，模糊查询", example = "RULE_TEMP")
    private String ruleCode;

    /**
     * 规则名称，模糊查询
     */
    @Schema(description = "规则名称，模糊查询", example = "温度")
    private String ruleName;

    /**
     * 告警类型
     */
    @Pattern(regexp = "THRESHOLD|OFFLINE|CONTINUOUS|FLUCTUATION", message = "告警类型只能为 THRESHOLD、OFFLINE、CONTINUOUS 或 FLUCTUATION")
    @Schema(description = "告警类型：THRESHOLD、OFFLINE、CONTINUOUS、FLUCTUATION", example = "THRESHOLD")
    private String alarmType;

    /**
     * 适用设备类型
     */
    @Pattern(regexp = "AC|DC", message = "设备类型只能为AC或DC")
    @Schema(description = "适用设备类型：AC交流桩，DC直流桩", example = "DC")
    private String deviceType;

    /**
     * 指标名称
     */
    @Pattern(regexp = "temperature|voltage|current|network_delay|power|heartbeat",
            message = "指标名称只能为 temperature、voltage、current、network_delay、power 或 heartbeat")
    @Schema(description = "指标名称", example = "temperature")
    private String metricName;

    /**
     * 是否启用：0禁用，1启用
     */
    @Min(value = 0, message = "启用状态只能为0或1")
    @Max(value = 1, message = "启用状态只能为0或1")
    @Schema(description = "是否启用：0禁用，1启用", example = "1")
    private Integer enabled;

    /**
     * 告警等级：1一般，2重要，3严重
     */
    @Min(value = 1, message = "告警等级只能为1、2、3")
    @Max(value = 3, message = "告警等级只能为1、2、3")
    @Schema(description = "告警等级：1一般，2重要，3严重", example = "2")
    private Integer alarmLevel;
}
