package com.ccbcc.charge.monitor.module.alarm.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 修改告警规则请求参数
 */
@Data
@Schema(description = "修改告警规则请求参数")
public class AlarmRuleUpdateDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 规则名称
     */
    @NotBlank(message = "规则名称不能为空")
    @Size(max = 100, message = "规则名称长度不能超过100个字符")
    @Schema(description = "规则名称", example = "设备温度过高-已更新", requiredMode = Schema.RequiredMode.REQUIRED)
    private String ruleName;

    /**
     * 告警类型：THRESHOLD / OFFLINE / CONTINUOUS / FLUCTUATION
     */
    @NotBlank(message = "告警类型不能为空")
    @Pattern(regexp = "THRESHOLD|OFFLINE|CONTINUOUS|FLUCTUATION", message = "告警类型只能为 THRESHOLD、OFFLINE、CONTINUOUS 或 FLUCTUATION")
    @Schema(description = "告警类型：THRESHOLD阈值、OFFLINE离线、CONTINUOUS连续异常、FLUCTUATION波动异常",
            example = "THRESHOLD", requiredMode = Schema.RequiredMode.REQUIRED)
    private String alarmType;

    /**
     * 适用设备类型：不填表示全部，AC 交流桩，DC 直流桩
     */
    @Pattern(regexp = "^(AC|DC)?$", message = "设备类型只能为 AC 或 DC，或留空表示全部")
    @Schema(description = "适用设备类型：不填表示全部设备，AC交流桩，DC直流桩", example = "DC")
    private String deviceType;

    /**
     * 指标名称
     */
    @NotBlank(message = "指标名称不能为空")
    @Pattern(regexp = "temperature|voltage|current|network_delay|power|heartbeat",
            message = "指标名称只能为 temperature、voltage、current、network_delay、power 或 heartbeat")
    @Schema(description = "指标名称：temperature / voltage / current / network_delay / power / heartbeat",
            example = "temperature", requiredMode = Schema.RequiredMode.REQUIRED)
    private String metricName;

    /**
     * 运算符
     */
    @NotBlank(message = "运算符不能为空")
    @Pattern(regexp = ">|>=|<|<=|=|!=", message = "运算符只能为 >、>=、<、<=、= 或 !=")
    @Schema(description = "运算符：>、>=、<、<=、=、!=", example = ">=", requiredMode = Schema.RequiredMode.REQUIRED)
    private String operator;

    /**
     * 阈值
     */
    @DecimalMin(value = "0.00", message = "阈值不能小于0")
    @Schema(description = "阈值", example = "90.00", requiredMode = Schema.RequiredMode.REQUIRED)
    private BigDecimal thresholdValue;

    /**
     * 窗口大小（最近 N 次数据）
     */
    @Min(value = 1, message = "窗口大小不能小于1")
    @Schema(description = "窗口大小，最近 N 次数据，当前阶段默认 1", example = "1")
    private Integer windowSize;

    /**
     * 触发次数（窗口内至少 N 次异常才触发）
     */
    @Min(value = 1, message = "触发次数不能小于1")
    @Schema(description = "触发次数，窗口内至少 N 次异常才触发，当前阶段默认 1", example = "1")
    private Integer triggerCount;

    /**
     * 告警等级：1一般，2重要，3严重
     */
    @Min(value = 1, message = "告警等级只能为1、2、3")
    @Max(value = 3, message = "告警等级只能为1、2、3")
    @Schema(description = "告警等级：1一般，2重要，3严重", example = "2")
    private Integer alarmLevel;

    /**
     * 是否启用：0禁用，1启用
     */
    @Min(value = 0, message = "启用状态只能为0或1")
    @Max(value = 1, message = "启用状态只能为0或1")
    @Schema(description = "是否启用：0禁用，1启用", example = "1")
    private Integer enabled;

    /**
     * 备注
     */
    @Size(max = 255, message = "备注长度不能超过255个字符")
    @Schema(description = "备注", example = "修改后的告警规则描述")
    private String remark;
}
