package com.ccbcc.charge.monitor.module.device.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ccbcc.charge.monitor.module.device.entity.DeviceData;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 设备运行数据表 Mapper
 */
@Mapper
public interface DeviceDataMapper extends BaseMapper<DeviceData> {

    /**
     * 根据设备编号查询最近一条运行数据
     *
     * 用于 Redis 未命中时，从 MySQL 回源查询设备最新状态。
     *
     * @param deviceCode 设备编号
     * @return 最近一条设备运行数据
     */
    @Select("""
            SELECT *
            FROM device_data
            WHERE device_code = #{deviceCode}
            ORDER BY report_time DESC
            LIMIT 1
            """)
    DeviceData selectLatestByDeviceCode(@Param("deviceCode") String deviceCode);
}