package com.ccbcc.charge.monitor.module.alarm.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 告警规则分页列表返回
 */
@Data
@Accessors(chain = true)
@Schema(description = "告警规则分页列表返回")
public class AlarmRulePageVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 规则ID
     */
    @Schema(description = "规则ID", example = "1")
    private Long id;

    /**
     * 规则编码
     */
    @Schema(description = "规则编码", example = "RULE_TEMP_HIGH")
    private String ruleCode;

    /**
     * 规则名称
     */
    @Schema(description = "规则名称", example = "设备温度过高")
    private String ruleName;

    /**
     * 告警类型
     */
    @Schema(description = "告警类型：THRESHOLD阈值、OFFLINE离线、CONTINUOUS连续异常、FLUCTUATION波动异常", example = "THRESHOLD")
    private String alarmType;

    /**
     * 适用设备类型
     */
    @Schema(description = "适用设备类型：空表示全部，AC交流桩，DC直流桩", example = "DC")
    private String deviceType;

    /**
     * 指标名称
     */
    @Schema(description = "指标名称", example = "temperature")
    private String metricName;

    /**
     * 运算符
     */
    @Schema(description = "运算符", example = ">=")
    private String operator;

    /**
     * 阈值
     */
    @Schema(description = "阈值", example = "85.00")
    private BigDecimal thresholdValue;

    /**
     * 告警等级：1一般，2重要，3严重
     */
    @Schema(description = "告警等级：1一般，2重要，3严重", example = "2")
    private Integer alarmLevel;

    /**
     * 是否启用：0禁用，1启用
     */
    @Schema(description = "是否启用：0禁用，1启用", example = "1")
    private Integer enabled;

    /**
     * 备注
     */
    @Schema(description = "备注", example = "当设备温度超过85℃时触发告警")
    private String remark;

    /**
     * 创建时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "创建时间", example = "2026-06-26 09:00:00")
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "更新时间", example = "2026-06-26 14:30:00")
    private LocalDateTime updateTime;
}
