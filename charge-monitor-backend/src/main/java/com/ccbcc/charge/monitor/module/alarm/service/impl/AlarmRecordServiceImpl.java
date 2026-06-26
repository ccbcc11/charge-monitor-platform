package com.ccbcc.charge.monitor.module.alarm.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ccbcc.charge.monitor.common.exception.BusinessException;
import com.ccbcc.charge.monitor.common.result.PageResult;
import com.ccbcc.charge.monitor.common.result.ResultCode;
import com.ccbcc.charge.monitor.module.alarm.dto.AlarmRecordPageQueryDTO;
import com.ccbcc.charge.monitor.module.alarm.entity.AlarmRecord;
import com.ccbcc.charge.monitor.module.alarm.mapper.AlarmRecordMapper;
import com.ccbcc.charge.monitor.module.alarm.service.AlarmRecordService;
import com.ccbcc.charge.monitor.module.alarm.vo.AlarmRecordDetailVO;
import com.ccbcc.charge.monitor.module.alarm.vo.AlarmRecordPageVO;
import com.ccbcc.charge.monitor.module.device.entity.DeviceInfo;
import com.ccbcc.charge.monitor.module.device.mapper.DeviceInfoMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

/**
 * 告警记录服务实现类
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AlarmRecordServiceImpl implements AlarmRecordService {

    private final AlarmRecordMapper alarmRecordMapper;
    private final DeviceInfoMapper deviceInfoMapper;
    private final RedisTemplate<String, Object> redisTemplate;

    /**
     * Redis Key：当前存在未恢复告警的设备集合
     */
    private static final String DEVICE_ALARM_SET_KEY = "device:alarm:set";

    /**
     * 告警状态：未确认
     */
    private static final Integer ALARM_STATUS_UNACK = 0;

    /**
     * 告警状态：已确认
     */
    private static final Integer ALARM_STATUS_ACKED = 1;

    /**
     * 告警状态：已恢复
     */
    private static final Integer ALARM_STATUS_RECOVERED = 2;

    @Override
    public PageResult<AlarmRecordPageVO> pageAlarmRecord(AlarmRecordPageQueryDTO queryDTO) {

        Page<AlarmRecord> page = new Page<>(queryDTO.getPageNo(), queryDTO.getPageSize());

        LambdaQueryWrapper<AlarmRecord> wrapper = new LambdaQueryWrapper<AlarmRecord>()
                .like(StringUtils.hasText(queryDTO.getDeviceCode()),
                        AlarmRecord::getDeviceCode,
                        queryDTO.getDeviceCode())
                .eq(StringUtils.hasText(queryDTO.getAlarmType()),
                        AlarmRecord::getAlarmType,
                        queryDTO.getAlarmType())
                .eq(StringUtils.hasText(queryDTO.getAlarmMetric()),
                        AlarmRecord::getAlarmMetric,
                        queryDTO.getAlarmMetric())
                .eq(queryDTO.getAlarmLevel() != null,
                        AlarmRecord::getAlarmLevel,
                        queryDTO.getAlarmLevel())
                .eq(queryDTO.getAlarmStatus() != null,
                        AlarmRecord::getAlarmStatus,
                        queryDTO.getAlarmStatus())
                .ge(queryDTO.getStartTime() != null,
                        AlarmRecord::getLastTime,
                        queryDTO.getStartTime())
                .le(queryDTO.getEndTime() != null,
                        AlarmRecord::getLastTime,
                        queryDTO.getEndTime())
                .orderByDesc(AlarmRecord::getLastTime)
                .orderByDesc(AlarmRecord::getId);

        Page<AlarmRecord> resultPage = alarmRecordMapper.selectPage(page, wrapper);

        List<AlarmRecordPageVO> records = resultPage.getRecords()
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
    public AlarmRecordDetailVO getAlarmRecordDetail(Long id) {

        AlarmRecord alarmRecord = getAlarmRecordById(id);

        AlarmRecordDetailVO vo = convertToDetailVO(alarmRecord);

        /*
         * 查询设备基础信息，用于补充设备名称、站点、区域。
         *
         * 注意：
         * 这里不强制要求 device_info 一定存在。
         * 因为设备可能已经被逻辑删除，但历史告警仍然需要能查询。
         */
        DeviceInfo deviceInfo = deviceInfoMapper.selectById(alarmRecord.getDeviceId());

        if (deviceInfo != null) {
            vo.setDeviceName(deviceInfo.getDeviceName())
                    .setStationName(deviceInfo.getStationName())
                    .setRegion(deviceInfo.getRegion());
        }

        return vo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void ackAlarmRecord(Long id) {

        AlarmRecord alarmRecord = getAlarmRecordById(id);

        /*
         * 已恢复的告警不能再确认。
         */
        if (Objects.equals(alarmRecord.getAlarmStatus(), ALARM_STATUS_RECOVERED)) {
            throw new BusinessException(ResultCode.BUSINESS_ERROR, "告警已恢复，不能确认");
        }

        /*
         * 已确认的告警重复确认，直接返回成功。
         */
        if (Objects.equals(alarmRecord.getAlarmStatus(), ALARM_STATUS_ACKED)) {
            return;
        }

        /*
         * 未确认 -> 已确认
         */
        AlarmRecord update = new AlarmRecord();
        update.setId(id);
        update.setAlarmStatus(ALARM_STATUS_ACKED);
        update.setUpdateTime(LocalDateTime.now());

        alarmRecordMapper.updateById(update);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void recoverAlarmRecord(Long id) {

        AlarmRecord alarmRecord = getAlarmRecordById(id);

        /*
         * 已恢复的告警重复恢复，直接返回成功。
         */
        if (Objects.equals(alarmRecord.getAlarmStatus(), ALARM_STATUS_RECOVERED)) {
            return;
        }

        /*
         * 未确认/已确认 -> 已恢复
         */
        LocalDateTime now = LocalDateTime.now();

        AlarmRecord update = new AlarmRecord();
        update.setId(id);
        update.setAlarmStatus(ALARM_STATUS_RECOVERED);
        update.setRecoverTime(now);
        update.setUpdateTime(now);

        alarmRecordMapper.updateById(update);

        /*
         * 如果该设备已经没有其他未恢复告警，则从 Redis 当前告警设备集合中移除。
         */
        removeDeviceFromAlarmSetIfNecessary(alarmRecord.getDeviceCode());
    }

    /**
     * 根据ID查询告警记录
     */
    private AlarmRecord getAlarmRecordById(Long id) {
        if (id == null) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "告警ID不能为空");
        }

        AlarmRecord alarmRecord = alarmRecordMapper.selectById(id);

        if (alarmRecord == null) {
            throw new BusinessException(ResultCode.ALARM_NOT_FOUND);
        }

        return alarmRecord;
    }

    /**
     * Entity 转分页 VO
     */
    private AlarmRecordPageVO convertToPageVO(AlarmRecord alarmRecord) {
        AlarmRecordPageVO vo = new AlarmRecordPageVO();
        BeanUtils.copyProperties(alarmRecord, vo);
        return vo;
    }

    /**
     * Entity 转详情 VO
     */
    private AlarmRecordDetailVO convertToDetailVO(AlarmRecord alarmRecord) {
        AlarmRecordDetailVO vo = new AlarmRecordDetailVO();
        BeanUtils.copyProperties(alarmRecord, vo);
        return vo;
    }

    /**
     * 如果设备没有其他未恢复告警，则从 Redis 告警设备集合中移除
     */
    private void removeDeviceFromAlarmSetIfNecessary(String deviceCode) {
        if (!StringUtils.hasText(deviceCode)) {
            return;
        }

        Long unrecoveredCount = alarmRecordMapper.countUnrecoveredByDeviceCode(deviceCode);

        if (unrecoveredCount != null && unrecoveredCount > 0) {
            return;
        }

        try {
            redisTemplate.opsForSet().remove(DEVICE_ALARM_SET_KEY, deviceCode);
        } catch (Exception e) {
            log.warn("从Redis告警设备集合移除设备失败，deviceCode={}", deviceCode, e);
        }
    }
}