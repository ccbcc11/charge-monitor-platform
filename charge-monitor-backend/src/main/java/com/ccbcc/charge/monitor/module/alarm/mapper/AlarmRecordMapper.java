package com.ccbcc.charge.monitor.module.alarm.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ccbcc.charge.monitor.module.alarm.entity.AlarmRecord;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 告警记录表 Mapper
 */
@Mapper
public interface AlarmRecordMapper extends BaseMapper<AlarmRecord> {

    /**
     * 查询同一设备、同一告警类型、同一告警指标下尚未恢复的告警
     *
     * 用于告警去重：
     * 如果已经存在未确认或已确认的同类告警，则不重复新增，只更新次数和最近发生时间。
     *
     * @param deviceId    设备ID
     * @param alarmType   告警类型
     * @param alarmMetric 告警指标
     * @return 未恢复告警记录
     */
    @Select("""
            SELECT *
            FROM alarm_record
            WHERE device_id = #{deviceId}
              AND alarm_type = #{alarmType}
              AND alarm_metric = #{alarmMetric}
              AND alarm_status IN (0, 1)
              AND deleted = 0
            ORDER BY last_time DESC
            LIMIT 1
            """)
    AlarmRecord selectUnrecoveredAlarm(@Param("deviceId") Long deviceId,
                                       @Param("alarmType") String alarmType,
                                       @Param("alarmMetric") String alarmMetric);

    /**
     * 统计某设备是否还存在未恢复告警
     *
     * 用于恢复告警后判断是否需要从 Redis 的 device:alarm:set 中移除该设备。
     *
     * @param deviceCode 设备编号
     * @return 未恢复告警数量
     */
    @Select("""
            SELECT COUNT(1)
            FROM alarm_record
            WHERE device_code = #{deviceCode}
              AND alarm_status IN (0, 1)
              AND deleted = 0
            """)
    Long countUnrecoveredByDeviceCode(@Param("deviceCode") String deviceCode);
}