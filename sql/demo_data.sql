-- =========================================================
-- 新能源充电设施运行监测与智能告警平台
-- 额外演示数据脚本：demo_data.sql
--
-- 使用方式：
--   1. 先执行 init.sql 完成建库建表和基础数据初始化
--   2. 再执行本脚本，补充更多演示数据
--
-- 注意：
--   本脚本不会清空已有数据，仅追加插入。
--   如果 ID 冲突，请修改 ID 值或先删除已有数据。
-- =========================================================

USE charge_monitor;

-- =========================================================
-- 补充告警记录（各种状态的示例）
-- =========================================================

-- 已确认但未恢复的告警
INSERT INTO alarm_record (
    alarm_no, device_id, device_code, alarm_type, alarm_metric, alarm_level,
    current_value, threshold_value, alarm_message, alarm_status,
    first_time, last_time, alarm_count, work_order_generated, dedup_key
)
VALUES
    (
        CONCAT('AL', DATE_FORMAT(NOW(), '%Y%m%d%H%i%s'), '0002'),
        1, 'CP-0001', 'THRESHOLD', 'voltage', 2,
        170.50, 180.00,
        '设备电压低于阈值，当前电压170.50V，阈值180.00V',
        1, NOW(), NOW(), 2, 0, 'CP-0001:THRESHOLD:voltage'
    );

-- 已恢复的告警（历史告警）
INSERT INTO alarm_record (
    alarm_no, device_id, device_code, alarm_type, alarm_metric, alarm_level,
    current_value, threshold_value, alarm_message, alarm_status,
    first_time, last_time, recover_time, alarm_count, work_order_generated, dedup_key
)
VALUES
    (
        CONCAT('AL', DATE_FORMAT(NOW(), '%Y%m%d%H%i%s'), '0003'),
        2, 'CP-0002', 'THRESHOLD', 'network_delay', 1,
        250.00, 200.00,
        '设备网络延迟超过阈值，当前延迟250ms，阈值200ms',
        2, DATE_SUB(NOW(), INTERVAL 1 HOUR), DATE_SUB(NOW(), INTERVAL 1 HOUR), NOW(),
        1, 0, 'CP-0002:THRESHOLD:network_delay'
    );

-- =========================================================
-- 补充更多历史运行数据
-- =========================================================
INSERT INTO device_data (
    device_id, device_code, voltage, current_value, power, temperature,
    soc, network_delay, fault_code, report_time
)
VALUES
    (1, 'CP-0001', 220.80, 31.50, 6.90, 43.50, 70.00,  78, 'NORMAL',    DATE_SUB(NOW(), INTERVAL 2 HOUR)),
    (1, 'CP-0001', 221.20, 32.00, 7.10, 44.80, 69.00,  82, 'NORMAL',    DATE_SUB(NOW(), INTERVAL 1 HOUR)),
    (2, 'CP-0002', 219.50, 29.80, 6.50, 47.20, 56.00,  92, 'NORMAL',    DATE_SUB(NOW(), INTERVAL 2 HOUR)),
    (2, 'CP-0002', 220.00, 30.50, 6.70, 46.90, 55.50,  88, 'NORMAL',    DATE_SUB(NOW(), INTERVAL 1 HOUR)),
    (5, 'CP-0005', 220.50, 16.80, 3.70, 40.10, 59.00,  72, 'NORMAL',    DATE_SUB(NOW(), INTERVAL 2 HOUR)),
    (5, 'CP-0005', 219.90, 16.20, 3.60, 39.80, 60.50,  68, 'NORMAL',    DATE_SUB(NOW(), INTERVAL 1 HOUR));
