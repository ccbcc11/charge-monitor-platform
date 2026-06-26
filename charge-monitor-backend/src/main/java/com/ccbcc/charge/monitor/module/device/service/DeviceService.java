package com.ccbcc.charge.monitor.module.device.service;

import com.ccbcc.charge.monitor.common.result.PageResult;
import com.ccbcc.charge.monitor.module.device.dto.DeviceCreateDTO;
import com.ccbcc.charge.monitor.module.device.dto.DevicePageQueryDTO;
import com.ccbcc.charge.monitor.module.device.dto.DeviceUpdateDTO;
import com.ccbcc.charge.monitor.module.device.vo.DeviceDetailVO;
import com.ccbcc.charge.monitor.module.device.vo.DevicePageVO;

/**
 * 设备管理服务接口
 */
public interface DeviceService {

    /**
     * 新增设备
     *
     * @param createDTO 新增设备请求参数
     * @return 设备ID
     */
    Long createDevice(DeviceCreateDTO createDTO);

    /**
     * 修改设备
     *
     * @param id        设备ID
     * @param updateDTO 修改设备请求参数
     */
    void updateDevice(Long id, DeviceUpdateDTO updateDTO);

    /**
     * 删除设备
     *
     * @param id 设备ID
     */
    void deleteDevice(Long id);

    /**
     * 分页查询设备
     *
     * @param queryDTO 查询参数
     * @return 分页结果
     */
    PageResult<DevicePageVO> pageDevice(DevicePageQueryDTO queryDTO);

    /**
     * 查询设备详情
     *
     * @param id 设备ID
     * @return 设备详情
     */
    DeviceDetailVO getDeviceDetail(Long id);
}