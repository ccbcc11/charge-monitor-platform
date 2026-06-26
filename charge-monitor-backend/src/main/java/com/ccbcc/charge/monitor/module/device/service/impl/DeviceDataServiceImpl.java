package com.ccbcc.charge.monitor.module.device.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ccbcc.charge.monitor.common.exception.BusinessException;
import com.ccbcc.charge.monitor.common.result.PageResult;
import com.ccbcc.charge.monitor.common.result.ResultCode;
import com.ccbcc.charge.monitor.module.alarm.entity.AlarmRecord;
import com.ccbcc.charge.monitor.module.alarm.mapper.AlarmRecordMapper;
import com.ccbcc.charge.monitor.module.device.dto.DeviceDataHistoryQueryDTO;
import com.ccbcc.charge.monitor.module.device.dto.DeviceDataReportDTO;
import com.ccbcc.charge.monitor.module.device.entity.DeviceData;
import com.ccbcc.charge.monitor.module.device.entity.DeviceInfo;
import com.ccbcc.charge.monitor.module.device.mapper.DeviceDataMapper;
import com.ccbcc.charge.monitor.module.device.mapper.DeviceInfoMapper;
import com.ccbcc.charge.monitor.module.device.service.DeviceDataService;
import com.ccbcc.charge.monitor.module.device.vo.DeviceDataHistoryVO;
import com.ccbcc.charge.monitor.module.device.vo.DeviceDataReportVO;
import com.ccbcc.charge.monitor.module.device.vo.DeviceLatestStatusVO;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 设备运行数据服务实现类
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DeviceDataServiceImpl implements DeviceDataService {

    private final DeviceInfoMapper deviceInfoMapper;
    private final DeviceDataMapper deviceDataMapper;
    private final AlarmRecordMapper alarmRecordMapper;
    private final RedisTemplate<String, Object> redisTemplate;
    private final ObjectMapper objectMapper;

    private static final String DEVICE_STATUS_KEY_PREFIX = "device:status:";
    private static final String DEVICE_HEARTBEAT_KEY_PREFIX = "device:heartbeat:";
    private static final String DEVICE_ONLINE_SET_KEY = "device:online:set";
    private static final String DEVICE_ALARM_SET_KEY = "device:alarm:set";

    private static final Integer ONLINE_STATUS_ONLINE = 1;
    private static final Integer RUNNING_STATUS_DISABLED = 0;

    private static final String ALARM_TYPE_THRESHOLD = "THRESHOLD";

    private static final String METRIC_TEMPERATURE = "temperature";
    private static final String METRIC_VOLTAGE = "voltage";
    private static final String METRIC_NETWORK_DELAY = "network_delay";

    private static final BigDecimal TEMPERATURE_THRESHOLD = BigDecimal.valueOf(80.00);
    private static final BigDecimal VOLTAGE_LOW_THRESHOLD = BigDecimal.valueOf(180.00);
    private static final BigDecimal NETWORK_DELAY_THRESHOLD = BigDecimal.valueOf(200.00);

    @Override
    @Transactional(rollbackFor = Exception.class)
    public DeviceDataReportVO report(DeviceDataReportDTO reportDTO) {

        /*
         * 1. 校验设备是否存在
         */
        DeviceInfo deviceInfo = getDeviceByCode(reportDTO.getDeviceCode());

        /*
         * 2. 判断设备是否停用
         */
        if (Objects.equals(deviceInfo.getRunningStatus(), RUNNING_STATUS_DISABLED)) {
            throw new BusinessException(ResultCode.DEVICE_DISABLED);
        }

        /*
         * 3. 保存设备运行数据到 MySQL
         */
        DeviceData deviceData = new DeviceData();
        BeanUtils.copyProperties(reportDTO, deviceData);

        deviceData.setDeviceId(deviceInfo.getId());
        deviceData.setDeviceCode(deviceInfo.getDeviceCode());
        deviceData.setCreateTime(LocalDateTime.now());

        deviceDataMapper.insert(deviceData);

        /*
         * 4. 更新设备在线状态和最近心跳
         */
        DeviceInfo updateDevice = new DeviceInfo();
        updateDevice.setId(deviceInfo.getId());
        updateDevice.setOnlineStatus(ONLINE_STATUS_ONLINE);
        updateDevice.setLastHeartbeat(reportDTO.getReportTime());
        updateDevice.setUpdateTime(LocalDateTime.now());

        deviceInfoMapper.updateById(updateDevice);

        deviceInfo.setOnlineStatus(ONLINE_STATUS_ONLINE);
        deviceInfo.setLastHeartbeat(reportDTO.getReportTime());

        /*
         * 5. 写入 Redis 最新状态、心跳、在线集合
         */
        DeviceLatestStatusVO latestStatusVO = buildLatestStatusVO(deviceInfo, deviceData, true);
        writeLatestStatusToRedis(deviceInfo.getDeviceCode(), latestStatusVO, reportDTO.getReportTime());

        /*
         * 6. 执行 MVP 基础阈值告警
         */
        List<Long> alarmIds = detectThresholdAlarms(deviceInfo, deviceData);

        /*
         * 7. 返回上报结果
         */
        return new DeviceDataReportVO()
                .setDataId(deviceData.getId())
                .setDeviceCode(deviceInfo.getDeviceCode())
                .setAlarmTriggered(!alarmIds.isEmpty())
                .setAlarmIds(alarmIds);
    }

    @Override
    public DeviceLatestStatusVO getLatestStatus(String deviceCode) {

        if (!StringUtils.hasText(deviceCode)) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "设备编号不能为空");
        }

        DeviceInfo deviceInfo = getDeviceByCode(deviceCode);

        /*
         * 1. 优先查询 Redis
         */
        String redisKey = DEVICE_STATUS_KEY_PREFIX + deviceCode;

        try {
            Object cached = redisTemplate.opsForValue().get(redisKey);

            if (cached != null) {
                DeviceLatestStatusVO vo = objectMapper.convertValue(cached, DeviceLatestStatusVO.class);
                vo.setFromCache(true);
                return vo;
            }
        } catch (Exception e) {
            log.warn("查询设备最新状态Redis缓存失败，deviceCode={}", deviceCode, e);
        }

        /*
         * 2. Redis 未命中，查询 MySQL 最近一条运行数据
         */
        DeviceData latestData = deviceDataMapper.selectLatestByDeviceCode(deviceCode);

        DeviceLatestStatusVO vo = buildLatestStatusVO(deviceInfo, latestData, false);

        /*
         * 3. 如果 MySQL 有历史数据，则回写 Redis
         */
        if (latestData != null) {
            writeLatestStatusToRedis(deviceCode, vo, latestData.getReportTime());
        }

        return vo;
    }

    @Override
    public PageResult<DeviceDataHistoryVO> pageHistory(DeviceDataHistoryQueryDTO queryDTO) {

        /*
         * 1. 校验设备是否存在
         */
        getDeviceByCode(queryDTO.getDeviceCode());

        /*
         * 2. 分页查询历史运行数据
         */
        Page<DeviceData> page = new Page<>(queryDTO.getPageNo(), queryDTO.getPageSize());

        LambdaQueryWrapper<DeviceData> wrapper = new LambdaQueryWrapper<DeviceData>()
                .eq(DeviceData::getDeviceCode, queryDTO.getDeviceCode())
                .ge(queryDTO.getStartTime() != null,
                        DeviceData::getReportTime,
                        queryDTO.getStartTime())
                .le(queryDTO.getEndTime() != null,
                        DeviceData::getReportTime,
                        queryDTO.getEndTime())
                .orderByDesc(DeviceData::getReportTime);

        Page<DeviceData> resultPage = deviceDataMapper.selectPage(page, wrapper);

        List<DeviceDataHistoryVO> records = resultPage.getRecords()
                .stream()
                .map(this::convertToHistoryVO)
                .toList();

        return new PageResult<>(
                records,
                resultPage.getTotal(),
                resultPage.getCurrent(),
                resultPage.getSize(),
                resultPage.getPages()
        );
    }

    /**
     * 根据设备编号查询设备
     */
    private DeviceInfo getDeviceByCode(String deviceCode) {
        DeviceInfo deviceInfo = deviceInfoMapper.selectOne(
                new LambdaQueryWrapper<DeviceInfo>()
                        .eq(DeviceInfo::getDeviceCode, deviceCode)
                        .last("LIMIT 1")
        );

        if (deviceInfo == null) {
            throw new BusinessException(ResultCode.DEVICE_NOT_FOUND);
        }

        return deviceInfo;
    }

    /**
     * 写入 Redis 最新状态
     */
    private void writeLatestStatusToRedis(String deviceCode,
                                          DeviceLatestStatusVO latestStatusVO,
                                          LocalDateTime heartbeatTime) {
        try {
            redisTemplate.opsForValue().set(DEVICE_STATUS_KEY_PREFIX + deviceCode, latestStatusVO);
            redisTemplate.opsForValue().set(DEVICE_HEARTBEAT_KEY_PREFIX + deviceCode, heartbeatTime);
            redisTemplate.opsForSet().add(DEVICE_ONLINE_SET_KEY, deviceCode);
        } catch (Exception e) {
            log.warn("写入设备最新状态Redis缓存失败，deviceCode={}", deviceCode, e);
        }
    }

    /**
     * 执行 MVP 阈值告警检测
     */
    private List<Long> detectThresholdAlarms(DeviceInfo deviceInfo, DeviceData deviceData) {
        List<Long> alarmIds = new ArrayList<>();

        Long temperatureAlarmId = handleTemperatureAlarm(deviceInfo, deviceData);
        if (temperatureAlarmId != null) {
            alarmIds.add(temperatureAlarmId);
        }

        Long voltageAlarmId = handleVoltageAlarm(deviceInfo, deviceData);
        if (voltageAlarmId != null) {
            alarmIds.add(voltageAlarmId);
        }

        Long networkDelayAlarmId = handleNetworkDelayAlarm(deviceInfo, deviceData);
        if (networkDelayAlarmId != null) {
            alarmIds.add(networkDelayAlarmId);
        }

        return alarmIds;
    }

    /**
     * 温度过高告警：temperature > 80
     */
    private Long handleTemperatureAlarm(DeviceInfo deviceInfo, DeviceData deviceData) {
        BigDecimal value = deviceData.getTemperature();

        if (value == null || value.compareTo(TEMPERATURE_THRESHOLD) <= 0) {
            return null;
        }

        return createOrUpdateThresholdAlarm(
                deviceInfo,
                METRIC_TEMPERATURE,
                3,
                value,
                TEMPERATURE_THRESHOLD,
                "设备温度超过阈值，当前温度" + value + "℃，阈值" + TEMPERATURE_THRESHOLD + "℃"
        );
    }

    /**
     * 电压过低告警：voltage < 180
     */
    private Long handleVoltageAlarm(DeviceInfo deviceInfo, DeviceData deviceData) {
        BigDecimal value = deviceData.getVoltage();

        if (value == null || value.compareTo(VOLTAGE_LOW_THRESHOLD) >= 0) {
            return null;
        }

        return createOrUpdateThresholdAlarm(
                deviceInfo,
                METRIC_VOLTAGE,
                2,
                value,
                VOLTAGE_LOW_THRESHOLD,
                "设备电压低于阈值，当前电压" + value + "V，阈值" + VOLTAGE_LOW_THRESHOLD + "V"
        );
    }

    /**
     * 网络延迟过高告警：networkDelay > 200
     */
    private Long handleNetworkDelayAlarm(DeviceInfo deviceInfo, DeviceData deviceData) {
        Integer value = deviceData.getNetworkDelay();

        if (value == null || BigDecimal.valueOf(value).compareTo(NETWORK_DELAY_THRESHOLD) <= 0) {
            return null;
        }

        return createOrUpdateThresholdAlarm(
                deviceInfo,
                METRIC_NETWORK_DELAY,
                1,
                BigDecimal.valueOf(value),
                NETWORK_DELAY_THRESHOLD,
                "设备网络延迟超过阈值，当前延迟" + value + "ms，阈值" + NETWORK_DELAY_THRESHOLD + "ms"
        );
    }

    /**
     * 新增或更新阈值告警
     */
    private Long createOrUpdateThresholdAlarm(DeviceInfo deviceInfo,
                                              String alarmMetric,
                                              Integer alarmLevel,
                                              BigDecimal currentValue,
                                              BigDecimal thresholdValue,
                                              String alarmMessage) {

        AlarmRecord existing = alarmRecordMapper.selectUnrecoveredAlarm(
                deviceInfo.getId(),
                ALARM_TYPE_THRESHOLD,
                alarmMetric
        );

        LocalDateTime now = LocalDateTime.now();

        /*
         * 1. 不存在未恢复告警：新增告警
         */
        if (existing == null) {
            AlarmRecord record = new AlarmRecord();

            record.setAlarmNo(generateAlarmNo());
            record.setDeviceId(deviceInfo.getId());
            record.setDeviceCode(deviceInfo.getDeviceCode());
            record.setAlarmType(ALARM_TYPE_THRESHOLD);
            record.setAlarmMetric(alarmMetric);
            record.setAlarmLevel(alarmLevel);
            record.setCurrentValue(currentValue);
            record.setThresholdValue(thresholdValue);
            record.setAlarmMessage(alarmMessage);
            record.setAlarmStatus(0);
            record.setFirstTime(now);
            record.setLastTime(now);
            record.setRecoverTime(null);
            record.setAlarmCount(1);
            record.setWorkOrderGenerated(0);
            record.setDedupKey(buildDedupKey(deviceInfo.getDeviceCode(), ALARM_TYPE_THRESHOLD, alarmMetric));
            record.setCreateTime(now);
            record.setUpdateTime(now);
            record.setDeleted(0);

            alarmRecordMapper.insert(record);

            addDeviceToAlarmSet(deviceInfo.getDeviceCode());

            return record.getId();
        }

        /*
         * 2. 已存在未恢复告警：只更新告警次数和最近发生时间
         */
        Integer oldCount = existing.getAlarmCount() == null ? 0 : existing.getAlarmCount();

        existing.setCurrentValue(currentValue);
        existing.setThresholdValue(thresholdValue);
        existing.setAlarmMessage(alarmMessage);
        existing.setLastTime(now);
        existing.setAlarmCount(oldCount + 1);
        existing.setUpdateTime(now);

        alarmRecordMapper.updateById(existing);

        addDeviceToAlarmSet(deviceInfo.getDeviceCode());

        return existing.getId();
    }

    /**
     * 生成告警编号
     *
     * 格式示例：
     * AL202606261955301230456
     */
    private String generateAlarmNo() {
        String timePart = LocalDateTime.now()
                .format(DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS"));

        int randomPart = ThreadLocalRandom.current().nextInt(1000, 10000);

        return "AL" + timePart + randomPart;
    }

    /**
     * 构建告警去重键
     *
     * 格式：
     * CP-0001:THRESHOLD:temperature
     */
    private String buildDedupKey(String deviceCode, String alarmType, String alarmMetric) {
        return deviceCode + ":" + alarmType + ":" + alarmMetric;
    }

    /**
     * 将设备加入 Redis 告警设备集合
     */
    private void addDeviceToAlarmSet(String deviceCode) {
        try {
            redisTemplate.opsForSet().add(DEVICE_ALARM_SET_KEY, deviceCode);
        } catch (Exception e) {
            log.warn("写入Redis告警设备集合失败，deviceCode={}", deviceCode, e);
        }
    }

    /**
     * 构建设备最新状态 VO
     */
    private DeviceLatestStatusVO buildLatestStatusVO(DeviceInfo deviceInfo,
                                                     DeviceData deviceData,
                                                     Boolean fromCache) {
        DeviceLatestStatusVO vo = new DeviceLatestStatusVO()
                .setDeviceCode(deviceInfo.getDeviceCode())
                .setDeviceName(deviceInfo.getDeviceName())
                .setOnlineStatus(deviceInfo.getOnlineStatus())
                .setRunningStatus(deviceInfo.getRunningStatus())
                .setFromCache(fromCache);

        if (deviceData != null) {
            vo.setVoltage(deviceData.getVoltage())
                    .setCurrentValue(deviceData.getCurrentValue())
                    .setPower(deviceData.getPower())
                    .setTemperature(deviceData.getTemperature())
                    .setSoc(deviceData.getSoc())
                    .setNetworkDelay(deviceData.getNetworkDelay())
                    .setFaultCode(deviceData.getFaultCode())
                    .setReportTime(deviceData.getReportTime());
        }

        return vo;
    }

    /**
     * Entity 转历史数据 VO
     */
    private DeviceDataHistoryVO convertToHistoryVO(DeviceData deviceData) {
        DeviceDataHistoryVO vo = new DeviceDataHistoryVO();
        BeanUtils.copyProperties(deviceData, vo);
        return vo;
    }
}