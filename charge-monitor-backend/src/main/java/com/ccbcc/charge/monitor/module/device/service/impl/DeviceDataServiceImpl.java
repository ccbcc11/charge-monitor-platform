package com.ccbcc.charge.monitor.module.device.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ccbcc.charge.monitor.common.constants.RedisKeyConstants;
import com.ccbcc.charge.monitor.common.exception.BusinessException;
import com.ccbcc.charge.monitor.common.result.PageResult;
import com.ccbcc.charge.monitor.common.result.ResultCode;
import com.ccbcc.charge.monitor.module.alarm.mq.message.DeviceDataReportMessage;
import com.ccbcc.charge.monitor.module.alarm.mq.producer.DeviceDataAlarmProducer;
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
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Objects;

/**
 * 设备运行数据服务实现类
 *
 * 当前职责：
 * 1. 设备运行数据上报
 * 2. MySQL 历史数据入库
 * 3. 设备在线状态与最近心跳更新
 * 4. Redis 最新状态、心跳、在线集合维护
 * 5. 事务提交成功后发送 RabbitMQ 消息，由消费者异步执行告警检测
 * 6. 查询最新状态与历史数据
 *
 * 说明：
 * 告警检测已正式迁移到 DeviceDataAlarmConsumer 异步消费，
 * 上报接口不再同步执行告警检测，避免阻塞 HTTP 响应。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DeviceDataServiceImpl implements DeviceDataService {

    private static final Integer ONLINE_STATUS_ONLINE = 1;

    /**
     * 运行状态：0 停用
     */
    private static final Integer RUNNING_STATUS_DISABLED = 0;

    /**
     * Redis 心跳时间格式
     */
    private static final DateTimeFormatter HEARTBEAT_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final DeviceInfoMapper deviceInfoMapper;
    private final DeviceDataMapper deviceDataMapper;
    private final RedisTemplate<String, Object> redisTemplate;
    private final ObjectMapper objectMapper;
    private final DeviceDataAlarmProducer deviceDataAlarmProducer;

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
         * 3. 确定本次心跳时间
         *
         * reportTime 是设备上报时间。
         * 如果设备端没有传，则使用服务端当前时间兜底。
         */
        LocalDateTime heartbeatTime = reportDTO.getReportTime() == null
                ? LocalDateTime.now()
                : reportDTO.getReportTime();

        /*
         * 4. 保存设备运行数据到 MySQL
         */
        DeviceData deviceData = new DeviceData();
        BeanUtils.copyProperties(reportDTO, deviceData);

        deviceData.setDeviceId(deviceInfo.getId());
        deviceData.setDeviceCode(deviceInfo.getDeviceCode());
        deviceData.setReportTime(heartbeatTime);
        deviceData.setCreateTime(LocalDateTime.now());

        deviceDataMapper.insert(deviceData);

        /*
         * 5. 更新设备在线状态和最近心跳
         */
        DeviceInfo updateDevice = new DeviceInfo();
        updateDevice.setId(deviceInfo.getId());
        updateDevice.setOnlineStatus(ONLINE_STATUS_ONLINE);
        updateDevice.setLastHeartbeat(heartbeatTime);
        updateDevice.setUpdateTime(LocalDateTime.now());

        deviceInfoMapper.updateById(updateDevice);

        /*
         * 同步更新当前内存中的 deviceInfo，便于后续构造 VO 和告警检测。
         */
        deviceInfo.setOnlineStatus(ONLINE_STATUS_ONLINE);
        deviceInfo.setLastHeartbeat(heartbeatTime);

        /*
         * 6. 写入 Redis 最新状态、心跳、在线集合
         */
        DeviceLatestStatusVO latestStatusVO = buildLatestStatusVO(deviceInfo, deviceData, true);
        writeLatestStatusToRedis(deviceInfo.getDeviceCode(), latestStatusVO, heartbeatTime);

        /*
         * 7. 事务提交成功后发送 MQ 消息
         *
         * 注意：
         * 这里不要在事务提交前直接发送。
         * 否则消费者可能先收到消息，但 device_data 事务还没提交，
         * 消费者根据 deviceDataId 查询时可能查不到数据。
         */
        DeviceDataReportMessage message = new DeviceDataReportMessage()
                .setDeviceDataId(deviceData.getId())
                .setDeviceId(deviceInfo.getId())
                .setDeviceCode(deviceInfo.getDeviceCode())
                .setReportTime(heartbeatTime);

        sendMqAfterTransactionCommit(message);

        /*
         * 8. 返回上报结果
         *
         * 告警检测已异步化，此处不再同步返回告警结果。
         * 告警结果以后以 alarm_record 查询为准。
         */
        return new DeviceDataReportVO()
                .setDataId(deviceData.getId())
                .setDeviceCode(deviceInfo.getDeviceCode())
                .setAlarmTriggered(false)
                .setAlarmIds(List.of());
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
        String redisKey = RedisKeyConstants.deviceStatus(deviceCode);

        try {
            Object cached = redisTemplate.opsForValue().get(redisKey);

            if (cached != null) {
                DeviceLatestStatusVO vo = objectMapper.convertValue(cached, DeviceLatestStatusVO.class);
                vo.setFromCache(true);
                return vo;
            }
        } catch (Exception e) {
            log.warn("查询设备最新状态 Redis 缓存失败，deviceCode={}", deviceCode, e);
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
            LocalDateTime heartbeatTime = latestData.getReportTime() == null
                    ? LocalDateTime.now()
                    : latestData.getReportTime();

            writeLatestStatusToRedis(deviceCode, vo, heartbeatTime);
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
        if (!StringUtils.hasText(deviceCode)) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "设备编号不能为空");
        }

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
     * 写入 Redis 最新状态、心跳、在线集合
     */
    private void writeLatestStatusToRedis(String deviceCode,
                                          DeviceLatestStatusVO latestStatusVO,
                                          LocalDateTime heartbeatTime) {
        try {
            /*
             * 1. 设备最新状态
             */
            redisTemplate.opsForValue()
                    .set(RedisKeyConstants.deviceStatus(deviceCode), latestStatusVO);

            /*
             * 2. 设备心跳
             *
             * 这里存 yyyy-MM-dd HH:mm:ss 字符串，
             * 方便离线检测任务用 StringRedisTemplate 读取。
             */
            String heartbeatText = heartbeatTime == null
                    ? LocalDateTime.now().format(HEARTBEAT_FORMATTER)
                    : heartbeatTime.format(HEARTBEAT_FORMATTER);

            redisTemplate.opsForValue()
                    .set(RedisKeyConstants.deviceHeartbeat(deviceCode), heartbeatText);

            /*
             * 3. 在线设备集合
             */
            redisTemplate.opsForSet()
                    .add(RedisKeyConstants.DEVICE_ONLINE_SET, deviceCode);

        } catch (Exception e) {
            log.warn("写入设备最新状态 Redis 缓存失败，deviceCode={}", deviceCode, e);
        }
    }

    /**
     * 事务提交成功后发送 MQ 消息
     *
     * 目的：
     * 保证 device_data、device_info 等数据库操作真正提交成功后，
     * 再通知消费者处理告警逻辑。
     */
    private void sendMqAfterTransactionCommit(DeviceDataReportMessage message) {
        if (message == null) {
            return;
        }

        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {

                @Override
                public void afterCommit() {
                    deviceDataAlarmProducer.sendDeviceDataReportMessage(message);
                }

                @Override
                public void afterCompletion(int status) {
                    if (status != STATUS_COMMITTED) {
                        log.warn(
                                "设备数据上报事务未提交成功，取消发送 MQ 消息，deviceDataId={}，deviceCode={}，status={}",
                                message.getDeviceDataId(),
                                message.getDeviceCode(),
                                status
                        );
                    }
                }
            });
            return;
        }

        /*
         * 理论上 report() 方法有 @Transactional，正常不会走到这里。
         * 这里作为兜底，防止未来复用该方法时没有事务上下文。
         */
        deviceDataAlarmProducer.sendDeviceDataReportMessage(message);
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