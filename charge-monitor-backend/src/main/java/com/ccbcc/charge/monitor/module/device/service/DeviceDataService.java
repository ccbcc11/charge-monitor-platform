package com.ccbcc.charge.monitor.module.device.service;

import com.ccbcc.charge.monitor.common.result.PageResult;
import com.ccbcc.charge.monitor.module.device.dto.DeviceDataHistoryQueryDTO;
import com.ccbcc.charge.monitor.module.device.dto.DeviceDataReportDTO;
import com.ccbcc.charge.monitor.module.device.vo.DeviceDataHistoryVO;
import com.ccbcc.charge.monitor.module.device.vo.DeviceDataReportVO;
import com.ccbcc.charge.monitor.module.device.vo.DeviceLatestStatusVO;

/**
 * 设备运行数据服务接口
 */
public interface DeviceDataService {

    /**
     * 上报设备运行数据
     *
     * @param reportDTO 设备运行数据上报参数
     * @return 上报结果
     */
    DeviceDataReportVO report(DeviceDataReportDTO reportDTO);

    /**
     * 查询设备最新状态
     *
     * @param deviceCode 设备编号
     * @return 设备最新状态
     */
    DeviceLatestStatusVO getLatestStatus(String deviceCode);

    /**
     * 分页查询设备历史运行数据
     *
     * @param queryDTO 查询参数
     * @return 分页结果
     */
    PageResult<DeviceDataHistoryVO> pageHistory(DeviceDataHistoryQueryDTO queryDTO);
}