package com.ccbcc.charge.monitor.module.device.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 设备运行数据表实体类
 */
@Data
@Accessors(chain = true)
@TableName("device_data")
public class DeviceData implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 设备ID
     */
    private Long deviceId;

    /**
     * 设备编号
     */
    private String deviceCode;

    /**
     * 电压，单位V
     */
    private BigDecimal voltage;

    /**
     * 电流，单位A
     *
     * 数据库字段为 current_value，避免使用 current 作为字段名。
     */
    @TableField("current_value")
    private BigDecimal currentValue;

    /**
     * 功率，单位kW
     */
    private BigDecimal power;

    /**
     * 温度，单位℃
     */
    private BigDecimal temperature;

    /**
     * 充电状态/电量百分比，单位%
     */
    private BigDecimal soc;

    /**
     * 网络延迟，单位ms
     */
    private Integer networkDelay;

    /**
     * 故障码，NORMAL表示正常
     */
    private String faultCode;

    /**
     * 设备上报时间
     */
    private LocalDateTime reportTime;

    /**
     * 入库时间
     */
    private LocalDateTime createTime;
}