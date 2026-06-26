package com.ccbcc.charge.monitor.module.device.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ccbcc.charge.monitor.module.device.entity.DeviceInfo;
import org.apache.ibatis.annotations.Mapper;

/**
 * 设备信息表 Mapper
 */
@Mapper
public interface DeviceInfoMapper extends BaseMapper<DeviceInfo> {
}