package com.ccbcc.charge.monitor.module.device.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ccbcc.charge.monitor.common.exception.BusinessException;
import com.ccbcc.charge.monitor.common.result.PageResult;
import com.ccbcc.charge.monitor.common.result.ResultCode;
import com.ccbcc.charge.monitor.module.device.dto.DeviceCreateDTO;
import com.ccbcc.charge.monitor.module.device.dto.DevicePageQueryDTO;
import com.ccbcc.charge.monitor.module.device.dto.DeviceUpdateDTO;
import com.ccbcc.charge.monitor.module.device.entity.DeviceInfo;
import com.ccbcc.charge.monitor.module.device.mapper.DeviceInfoMapper;
import com.ccbcc.charge.monitor.module.device.service.DeviceService;
import com.ccbcc.charge.monitor.module.device.vo.DeviceDetailVO;
import com.ccbcc.charge.monitor.module.device.vo.DevicePageVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 设备管理服务实现类
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DeviceServiceImpl implements DeviceService {

    private final DeviceInfoMapper deviceInfoMapper;
    private final RedisTemplate<String, Object> redisTemplate;

    /**
     * Redis Key：设备最新状态
     */
    private static final String DEVICE_STATUS_KEY_PREFIX = "device:status:";

    /**
     * Redis Key：设备心跳
     */
    private static final String DEVICE_HEARTBEAT_KEY_PREFIX = "device:heartbeat:";

    /**
     * Redis Key：在线设备集合
     */
    private static final String DEVICE_ONLINE_SET_KEY = "device:online:set";

    /**
     * Redis Key：告警设备集合
     */
    private static final String DEVICE_ALARM_SET_KEY = "device:alarm:set";

    /**
     * 默认离线状态
     */
    private static final Integer ONLINE_STATUS_OFFLINE = 0;

    /**
     * 默认正常运行状态
     */
    private static final Integer RUNNING_STATUS_NORMAL = 1;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createDevice(DeviceCreateDTO createDTO) {

        /*
         * 1. 判断设备编号是否已存在
         */
        Long count = deviceInfoMapper.selectCount(
                new LambdaQueryWrapper<DeviceInfo>()
                        .eq(DeviceInfo::getDeviceCode, createDTO.getDeviceCode())
        );

        if (count != null && count > 0) {
            throw new BusinessException(ResultCode.DATA_ALREADY_EXISTS, "设备编号已存在");
        }

        /*
         * 2. DTO 转 Entity
         */
        DeviceInfo deviceInfo = new DeviceInfo();
        BeanUtils.copyProperties(createDTO, deviceInfo);

        /*
         * 3. 设置默认状态
         */
        deviceInfo.setOnlineStatus(ONLINE_STATUS_OFFLINE);

        if (deviceInfo.getRunningStatus() == null) {
            deviceInfo.setRunningStatus(RUNNING_STATUS_NORMAL);
        }

        LocalDateTime now = LocalDateTime.now();
        deviceInfo.setCreateTime(now);
        deviceInfo.setUpdateTime(now);
        deviceInfo.setDeleted(0);

        /*
         * 4. 写入数据库
         */
        deviceInfoMapper.insert(deviceInfo);

        return deviceInfo.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateDevice(Long id, DeviceUpdateDTO updateDTO) {

        /*
         * 1. 查询设备是否存在
         */
        DeviceInfo existing = deviceInfoMapper.selectById(id);

        if (existing == null) {
            throw new BusinessException(ResultCode.DEVICE_NOT_FOUND);
        }

        /*
         * 2. 更新允许修改的字段
         *
         * 不允许通过该接口修改：
         * deviceCode
         * onlineStatus
         * lastHeartbeat
         */
        DeviceInfo deviceInfo = new DeviceInfo();
        BeanUtils.copyProperties(updateDTO, deviceInfo);

        deviceInfo.setId(id);
        deviceInfo.setUpdateTime(LocalDateTime.now());

        deviceInfoMapper.updateById(deviceInfo);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteDevice(Long id) {

        /*
         * 1. 查询设备是否存在
         */
        DeviceInfo existing = deviceInfoMapper.selectById(id);

        if (existing == null) {
            throw new BusinessException(ResultCode.DEVICE_NOT_FOUND);
        }

        /*
         * 2. 逻辑删除
         *
         * DeviceInfo 中 deleted 字段已经标注 @TableLogic，
         * MyBatis-Plus 会把 deleteById 转成 update deleted = 1。
         */
        deviceInfoMapper.deleteById(id);

        /*
         * 3. 清理 Redis 中该设备的实时状态
         *
         * Redis 清理失败不影响数据库删除，所以只记录日志，不中断主流程。
         */
        clearDeviceRedisCache(existing.getDeviceCode());
    }

    @Override
    public PageResult<DevicePageVO> pageDevice(DevicePageQueryDTO queryDTO) {

        Page<DeviceInfo> page = new Page<>(queryDTO.getPageNo(), queryDTO.getPageSize());

        LambdaQueryWrapper<DeviceInfo> wrapper = new LambdaQueryWrapper<DeviceInfo>()
                .like(StringUtils.hasText(queryDTO.getDeviceCode()),
                        DeviceInfo::getDeviceCode,
                        queryDTO.getDeviceCode())
                .like(StringUtils.hasText(queryDTO.getDeviceName()),
                        DeviceInfo::getDeviceName,
                        queryDTO.getDeviceName())
                .like(StringUtils.hasText(queryDTO.getStationName()),
                        DeviceInfo::getStationName,
                        queryDTO.getStationName())
                .eq(StringUtils.hasText(queryDTO.getRegion()),
                        DeviceInfo::getRegion,
                        queryDTO.getRegion())
                .eq(StringUtils.hasText(queryDTO.getDeviceType()),
                        DeviceInfo::getDeviceType,
                        queryDTO.getDeviceType())
                .eq(queryDTO.getOnlineStatus() != null,
                        DeviceInfo::getOnlineStatus,
                        queryDTO.getOnlineStatus())
                .eq(queryDTO.getRunningStatus() != null,
                        DeviceInfo::getRunningStatus,
                        queryDTO.getRunningStatus())
                .orderByDesc(DeviceInfo::getCreateTime);

        Page<DeviceInfo> resultPage = deviceInfoMapper.selectPage(page, wrapper);

        List<DevicePageVO> records = resultPage.getRecords()
                .stream()
                .map(this::convertToPageVO)
                .toList();

        return new PageResult<>(
                records,
                resultPage.getTotal(),
                resultPage.getCurrent(),
                resultPage.getSize(),
                resultPage.getPages()
        );
    }

    @Override
    public DeviceDetailVO getDeviceDetail(Long id) {

        DeviceInfo deviceInfo = deviceInfoMapper.selectById(id);

        if (deviceInfo == null) {
            throw new BusinessException(ResultCode.DEVICE_NOT_FOUND);
        }

        return convertToDetailVO(deviceInfo);
    }

    /**
     * Entity 转分页 VO
     */
    private DevicePageVO convertToPageVO(DeviceInfo deviceInfo) {
        DevicePageVO vo = new DevicePageVO();
        BeanUtils.copyProperties(deviceInfo, vo);
        return vo;
    }

    /**
     * Entity 转详情 VO
     */
    private DeviceDetailVO convertToDetailVO(DeviceInfo deviceInfo) {
        DeviceDetailVO vo = new DeviceDetailVO();
        BeanUtils.copyProperties(deviceInfo, vo);
        return vo;
    }

    /**
     * 清理设备相关 Redis 缓存
     */
    private void clearDeviceRedisCache(String deviceCode) {
        if (!StringUtils.hasText(deviceCode)) {
            return;
        }

        try {
            redisTemplate.delete(DEVICE_STATUS_KEY_PREFIX + deviceCode);
            redisTemplate.delete(DEVICE_HEARTBEAT_KEY_PREFIX + deviceCode);
            redisTemplate.opsForSet().remove(DEVICE_ONLINE_SET_KEY, deviceCode);
            redisTemplate.opsForSet().remove(DEVICE_ALARM_SET_KEY, deviceCode);
        } catch (Exception e) {
            log.warn("清理设备Redis缓存失败，deviceCode={}", deviceCode, e);
        }
    }
}