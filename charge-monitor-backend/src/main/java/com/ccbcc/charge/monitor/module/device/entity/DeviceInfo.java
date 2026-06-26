package com.ccbcc.charge.monitor.module.device.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 设备信息表实体类
 */
@Data
@Accessors(chain = true)
@TableName("device_info")
public class DeviceInfo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 设备编号，唯一
     */
    private String deviceCode;

    /**
     * 设备名称
     */
    private String deviceName;

    /**
     * 所属站点
     */
    private String stationName;

    /**
     * 所属区域
     */
    private String region;

    /**
     * 设备类型：AC交流桩，DC直流桩
     */
    private String deviceType;

    /**
     * 额定功率，单位kW
     */
    private BigDecimal ratedPower;

    /**
     * 经度
     */
    private BigDecimal longitude;

    /**
     * 纬度
     */
    private BigDecimal latitude;

    /**
     * 在线状态：0离线，1在线
     */
    private Integer onlineStatus;

    /**
     * 运行状态：0停用，1正常，2异常
     */
    private Integer runningStatus;

    /**
     * 最近心跳时间
     */
    private LocalDateTime lastHeartbeat;

    /**
     * 负责人用户ID
     */
    private Long managerId;

    /**
     * 投运时间
     */
    private LocalDateTime installTime;

    /**
     * 备注
     */
    private String remark;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    private LocalDateTime updateTime;

    /**
     * 逻辑删除：0未删除，1已删除
     */
    @TableLogic
    private Integer deleted;
}