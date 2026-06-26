-- =========================================================
-- 新能源充电设施运行监测与智能告警平台
-- 数据库初始化脚本：init.sql
--
-- 数据库：charge_monitor
-- MySQL：8.0+
-- 字符集：utf8mb4
--
-- MVP 开发表：
-- 1. sys_user
-- 2. sys_role
-- 3. sys_user_role
-- 4. device_info
-- 5. device_data
-- 6. alarm_record
--
-- 第二阶段预留表：
-- 7. alarm_rule
-- 8. work_order
-- 9. work_order_log
-- 10. operation_log
-- 11. daily_report
-- =========================================================

CREATE DATABASE IF NOT EXISTS charge_monitor
    DEFAULT CHARACTER SET utf8mb4
    COLLATE utf8mb4_0900_ai_ci;

USE charge_monitor;

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

DROP TABLE IF EXISTS daily_report;
DROP TABLE IF EXISTS operation_log;
DROP TABLE IF EXISTS work_order_log;
DROP TABLE IF EXISTS work_order;
DROP TABLE IF EXISTS alarm_record;
DROP TABLE IF EXISTS alarm_rule;
DROP TABLE IF EXISTS device_data;
DROP TABLE IF EXISTS device_info;
DROP TABLE IF EXISTS sys_user_role;
DROP TABLE IF EXISTS sys_role;
DROP TABLE IF EXISTS sys_user;

SET FOREIGN_KEY_CHECKS = 1;

-- =========================================================
-- 1. 用户表
-- =========================================================
CREATE TABLE sys_user (
                          id              BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键ID',
                          username        VARCHAR(50)     NOT NULL COMMENT '用户名',
                          password        VARCHAR(128)    NOT NULL COMMENT '密码，MVP阶段使用SHA-256存储',
                          real_name       VARCHAR(50)     DEFAULT NULL COMMENT '真实姓名',
                          phone           VARCHAR(20)     DEFAULT NULL COMMENT '手机号',
                          email           VARCHAR(100)    DEFAULT NULL COMMENT '邮箱',
                          status          TINYINT         NOT NULL DEFAULT 1 COMMENT '状态：0禁用，1启用',
                          create_time     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                          update_time     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
                          deleted         TINYINT         NOT NULL DEFAULT 0 COMMENT '逻辑删除：0未删除，1已删除',
                          PRIMARY KEY (id),
                          UNIQUE KEY uk_username (username),
                          KEY idx_status (status)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci
    COMMENT = '用户表';

-- =========================================================
-- 2. 角色表
-- =========================================================
CREATE TABLE sys_role (
                          id              BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键ID',
                          role_code       VARCHAR(50)     NOT NULL COMMENT '角色编码：admin/operator/viewer',
                          role_name       VARCHAR(50)     NOT NULL COMMENT '角色名称',
                          description     VARCHAR(255)    DEFAULT NULL COMMENT '角色描述',
                          status          TINYINT         NOT NULL DEFAULT 1 COMMENT '状态：0禁用，1启用',
                          create_time     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                          update_time     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
                          deleted         TINYINT         NOT NULL DEFAULT 0 COMMENT '逻辑删除：0未删除，1已删除',
                          PRIMARY KEY (id),
                          UNIQUE KEY uk_role_code (role_code),
                          KEY idx_status (status)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci
    COMMENT = '角色表';

-- =========================================================
-- 3. 用户角色关联表
-- =========================================================
CREATE TABLE sys_user_role (
                               id              BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键ID',
                               user_id         BIGINT UNSIGNED NOT NULL COMMENT '用户ID',
                               role_id         BIGINT UNSIGNED NOT NULL COMMENT '角色ID',
                               create_time     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                               PRIMARY KEY (id),
                               UNIQUE KEY uk_user_role (user_id, role_id),
                               KEY idx_user_id (user_id),
                               KEY idx_role_id (role_id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci
    COMMENT = '用户角色关联表';

-- =========================================================
-- 4. 设备信息表
-- =========================================================
CREATE TABLE device_info (
                             id                  BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键ID',
                             device_code         VARCHAR(64)     NOT NULL COMMENT '设备编号，唯一',
                             device_name         VARCHAR(100)    NOT NULL COMMENT '设备名称',
                             station_name        VARCHAR(100)    DEFAULT NULL COMMENT '所属站点',
                             region              VARCHAR(50)     DEFAULT NULL COMMENT '所属区域',
                             device_type         VARCHAR(50)     DEFAULT NULL COMMENT '设备类型：AC交流桩，DC直流桩',
                             rated_power         DECIMAL(10,2)   DEFAULT NULL COMMENT '额定功率，单位kW',
                             longitude           DECIMAL(10,6)   DEFAULT NULL COMMENT '经度',
                             latitude            DECIMAL(10,6)   DEFAULT NULL COMMENT '纬度',
                             online_status       TINYINT         NOT NULL DEFAULT 0 COMMENT '在线状态：0离线，1在线',
                             running_status      TINYINT         NOT NULL DEFAULT 1 COMMENT '运行状态：0停用，1正常，2异常',
                             last_heartbeat      DATETIME        DEFAULT NULL COMMENT '最近心跳时间',
                             manager_id          BIGINT UNSIGNED DEFAULT NULL COMMENT '负责人用户ID',
                             install_time        DATETIME        DEFAULT NULL COMMENT '投运时间',
                             remark              VARCHAR(255)    DEFAULT NULL COMMENT '备注',
                             create_time         DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                             update_time         DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
                             deleted             TINYINT         NOT NULL DEFAULT 0 COMMENT '逻辑删除：0未删除，1已删除',
                             PRIMARY KEY (id),
                             UNIQUE KEY uk_device_code (device_code),
                             KEY idx_region (region),
                             KEY idx_online_status (online_status),
                             KEY idx_running_status (running_status),
                             KEY idx_manager_id (manager_id),
                             KEY idx_last_heartbeat (last_heartbeat)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci
    COMMENT = '设备信息表';

-- =========================================================
-- 5. 设备运行数据表
--
-- 注意：
-- 接口字段可以叫 current，
-- 数据库字段使用 current_value，避免和关键字/函数混淆。
-- =========================================================
CREATE TABLE device_data (
                             id                  BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键ID',
                             device_id           BIGINT UNSIGNED NOT NULL COMMENT '设备ID',
                             device_code         VARCHAR(64)     NOT NULL COMMENT '设备编号',
                             voltage             DECIMAL(10,2)   DEFAULT NULL COMMENT '电压，单位V',
                             current_value       DECIMAL(10,2)   DEFAULT NULL COMMENT '电流，单位A',
                             power               DECIMAL(10,2)   DEFAULT NULL COMMENT '功率，单位kW',
                             temperature         DECIMAL(10,2)   DEFAULT NULL COMMENT '温度，单位℃',
                             soc                 DECIMAL(5,2)    DEFAULT NULL COMMENT '充电状态/电量百分比，单位%',
                             network_delay       INT             DEFAULT NULL COMMENT '网络延迟，单位ms',
                             fault_code          VARCHAR(50)     DEFAULT 'NORMAL' COMMENT '故障码，NORMAL表示正常',
                             report_time         DATETIME        NOT NULL COMMENT '设备上报时间',
                             create_time         DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '入库时间',
                             PRIMARY KEY (id),
                             KEY idx_device_time (device_id, report_time),
                             KEY idx_code_time (device_code, report_time),
                             KEY idx_report_time (report_time),
                             KEY idx_fault_code (fault_code)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci
    COMMENT = '设备运行数据表';

-- =========================================================
-- 6. 告警规则表
-- 第二阶段重点开发，MVP阶段可先使用代码内置规则
-- =========================================================
CREATE TABLE alarm_rule (
                            id                  BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键ID',
                            rule_code           VARCHAR(64)     NOT NULL COMMENT '规则编码',
                            rule_name           VARCHAR(100)    NOT NULL COMMENT '规则名称',
                            alarm_type          VARCHAR(50)     NOT NULL COMMENT '告警类型：THRESHOLD阈值，OFFLINE离线，CONTINUOUS连续异常，FLUCTUATION波动异常',
                            device_type         VARCHAR(50)     DEFAULT NULL COMMENT '适用设备类型，NULL表示全部设备',
                            metric_name         VARCHAR(50)     NOT NULL COMMENT '指标名称：temperature/voltage/current/network_delay/power',
                            operator            VARCHAR(10)     NOT NULL COMMENT '运算符：>、>=、<、<=、=、!=',
                            threshold_value     DECIMAL(10,2)   DEFAULT NULL COMMENT '阈值',
                            window_size         INT             NOT NULL DEFAULT 1 COMMENT '窗口大小，例如最近N次',
                            trigger_count       INT             NOT NULL DEFAULT 1 COMMENT '触发次数，例如连续N次异常',
                            alarm_level         TINYINT         NOT NULL DEFAULT 1 COMMENT '告警等级：1一般，2重要，3严重',
                            enabled             TINYINT         NOT NULL DEFAULT 1 COMMENT '是否启用：0禁用，1启用',
                            remark              VARCHAR(255)    DEFAULT NULL COMMENT '备注',
                            create_time         DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                            update_time         DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
                            deleted             TINYINT         NOT NULL DEFAULT 0 COMMENT '逻辑删除：0未删除，1已删除',
                            PRIMARY KEY (id),
                            UNIQUE KEY uk_rule_code (rule_code),
                            KEY idx_enabled (enabled),
                            KEY idx_metric_name (metric_name),
                            KEY idx_alarm_type (alarm_type),
                            KEY idx_device_type (device_type)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci
    COMMENT = '告警规则表';

-- =========================================================
-- 7. 告警记录表
-- =========================================================
CREATE TABLE alarm_record (
                              id                      BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键ID',
                              alarm_no                VARCHAR(64)     NOT NULL COMMENT '告警编号',
                              device_id               BIGINT UNSIGNED NOT NULL COMMENT '设备ID',
                              device_code             VARCHAR(64)     NOT NULL COMMENT '设备编号',
                              alarm_type              VARCHAR(50)     NOT NULL COMMENT '告警类型：THRESHOLD/OFFLINE/CONTINUOUS/FLUCTUATION',
                              alarm_metric            VARCHAR(50)     NOT NULL COMMENT '告警指标：temperature/voltage/current/network_delay/power',
                              alarm_level             TINYINT         NOT NULL DEFAULT 1 COMMENT '告警等级：1一般，2重要，3严重',
                              current_value           DECIMAL(10,2)   DEFAULT NULL COMMENT '当前值',
                              threshold_value         DECIMAL(10,2)   DEFAULT NULL COMMENT '阈值',
                              alarm_message           VARCHAR(255)    NOT NULL COMMENT '告警描述',
                              alarm_status            TINYINT         NOT NULL DEFAULT 0 COMMENT '告警状态：0未确认，1已确认，2已恢复，3已关闭',
                              first_time              DATETIME        NOT NULL COMMENT '首次发生时间',
                              last_time               DATETIME        NOT NULL COMMENT '最近发生时间',
                              recover_time            DATETIME        DEFAULT NULL COMMENT '恢复时间',
                              alarm_count             INT             NOT NULL DEFAULT 1 COMMENT '发生次数',
                              work_order_generated    TINYINT         NOT NULL DEFAULT 0 COMMENT '是否已生成工单：0否，1是',
                              dedup_key               VARCHAR(200)    DEFAULT NULL COMMENT '告警去重键：设备+指标+类型',
                              create_time             DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                              update_time             DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
                              deleted                 TINYINT         NOT NULL DEFAULT 0 COMMENT '逻辑删除：0未删除，1已删除',
                              PRIMARY KEY (id),
                              UNIQUE KEY uk_alarm_no (alarm_no),
                              KEY idx_device_status (device_id, alarm_status),
                              KEY idx_code_status (device_code, alarm_status),
                              KEY idx_alarm_level (alarm_level),
                              KEY idx_alarm_type (alarm_type),
                              KEY idx_alarm_metric (alarm_metric),
                              KEY idx_first_time (first_time),
                              KEY idx_last_time (last_time),
                              KEY idx_dedup_key (dedup_key)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci
    COMMENT = '告警记录表';

-- =========================================================
-- 8. 工单表
-- 第二阶段开发：告警自动建单、派发、处理、关闭
-- =========================================================
CREATE TABLE work_order (
                            id                  BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键ID',
                            order_no            VARCHAR(64)     NOT NULL COMMENT '工单编号',
                            alarm_id            BIGINT UNSIGNED DEFAULT NULL COMMENT '关联告警ID，手动工单可为空',
                            device_id           BIGINT UNSIGNED NOT NULL COMMENT '设备ID',
                            device_code         VARCHAR(64)     NOT NULL COMMENT '设备编号',
                            title               VARCHAR(100)    NOT NULL COMMENT '工单标题',
                            description         VARCHAR(500)    DEFAULT NULL COMMENT '故障描述',
                            order_level         TINYINT         NOT NULL DEFAULT 1 COMMENT '工单等级：1一般，2重要，3紧急',
                            order_status        TINYINT         NOT NULL DEFAULT 0 COMMENT '工单状态：0待派发，1处理中，2待确认，3已关闭，4已驳回，5已取消',
                            create_user_id      BIGINT UNSIGNED DEFAULT NULL COMMENT '创建人ID',
                            assigner_id         BIGINT UNSIGNED DEFAULT NULL COMMENT '派发人ID',
                            handler_id          BIGINT UNSIGNED DEFAULT NULL COMMENT '处理人ID',
                            assign_time         DATETIME        DEFAULT NULL COMMENT '派发时间',
                            process_time        DATETIME        DEFAULT NULL COMMENT '处理时间',
                            close_time          DATETIME        DEFAULT NULL COMMENT '关闭时间',
                            handle_result       VARCHAR(500)    DEFAULT NULL COMMENT '处理结果',
                            remark              VARCHAR(255)    DEFAULT NULL COMMENT '备注',
                            create_time         DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                            update_time         DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
                            deleted             TINYINT         NOT NULL DEFAULT 0 COMMENT '逻辑删除：0未删除，1已删除',
                            PRIMARY KEY (id),
                            UNIQUE KEY uk_order_no (order_no),
                            KEY idx_alarm_id (alarm_id),
                            KEY idx_device_status (device_id, order_status),
                            KEY idx_code_status (device_code, order_status),
                            KEY idx_handler_status (handler_id, order_status),
                            KEY idx_create_time (create_time)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci
    COMMENT = '工单表';

-- =========================================================
-- 9. 工单流转日志表
-- =========================================================
CREATE TABLE work_order_log (
                                id                  BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键ID',
                                work_order_id       BIGINT UNSIGNED NOT NULL COMMENT '工单ID',
                                operator_id         BIGINT UNSIGNED DEFAULT NULL COMMENT '操作人ID',
                                operation_type      VARCHAR(50)     NOT NULL COMMENT '操作类型：CREATE/ASSIGN/PROCESS/CLOSE/REJECT/CANCEL',
                                before_status       TINYINT         DEFAULT NULL COMMENT '操作前状态',
                                after_status        TINYINT         DEFAULT NULL COMMENT '操作后状态',
                                remark              VARCHAR(500)    DEFAULT NULL COMMENT '操作备注',
                                create_time         DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '操作时间',
                                PRIMARY KEY (id),
                                KEY idx_work_order_id (work_order_id),
                                KEY idx_operator_id (operator_id),
                                KEY idx_create_time (create_time)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci
    COMMENT = '工单流转日志表';

-- =========================================================
-- 10. 操作日志表
-- 第二阶段开发：AOP记录关键操作
-- =========================================================
CREATE TABLE operation_log (
                               id                  BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键ID',
                               user_id             BIGINT UNSIGNED DEFAULT NULL COMMENT '操作用户ID',
                               username            VARCHAR(50)     DEFAULT NULL COMMENT '操作用户名',
                               module_name         VARCHAR(50)     DEFAULT NULL COMMENT '模块名称',
                               operation_type      VARCHAR(50)     DEFAULT NULL COMMENT '操作类型：新增/修改/删除/查询/登录/导出等',
                               request_method      VARCHAR(10)     DEFAULT NULL COMMENT '请求方式：GET/POST/PUT/DELETE',
                               request_uri         VARCHAR(255)    DEFAULT NULL COMMENT '请求URI',
                               request_params      TEXT            DEFAULT NULL COMMENT '请求参数',
                               ip_address          VARCHAR(64)     DEFAULT NULL COMMENT 'IP地址',
                               result_status       TINYINT         DEFAULT NULL COMMENT '执行结果：0失败，1成功',
                               error_msg           VARCHAR(1000)   DEFAULT NULL COMMENT '错误信息',
                               cost_time_ms        BIGINT          DEFAULT NULL COMMENT '耗时，单位ms',
                               create_time         DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                               PRIMARY KEY (id),
                               KEY idx_user_id (user_id),
                               KEY idx_username (username),
                               KEY idx_module_name (module_name),
                               KEY idx_operation_type (operation_type),
                               KEY idx_create_time (create_time)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci
    COMMENT = '操作日志表';

-- =========================================================
-- 11. 运行日报表
-- 第二阶段开发：Spring Task 定时统计
-- =========================================================
CREATE TABLE daily_report (
                              id                          BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键ID',
                              report_date                 DATE            NOT NULL COMMENT '统计日期',
                              device_total                INT             NOT NULL DEFAULT 0 COMMENT '设备总数',
                              online_count                INT             NOT NULL DEFAULT 0 COMMENT '在线设备数',
                              offline_count               INT             NOT NULL DEFAULT 0 COMMENT '离线设备数',
                              online_rate                 DECIMAL(5,2)    NOT NULL DEFAULT 0.00 COMMENT '在线率，单位%',
                              alarm_total                 INT             NOT NULL DEFAULT 0 COMMENT '告警总数',
                              serious_alarm_count         INT             NOT NULL DEFAULT 0 COMMENT '严重告警数',
                              important_alarm_count       INT             NOT NULL DEFAULT 0 COMMENT '重要告警数',
                              normal_alarm_count          INT             NOT NULL DEFAULT 0 COMMENT '一般告警数',
                              unhandled_alarm_count       INT             NOT NULL DEFAULT 0 COMMENT '未处理告警数',
                              work_order_total            INT             NOT NULL DEFAULT 0 COMMENT '工单总数',
                              closed_work_order_count     INT             NOT NULL DEFAULT 0 COMMENT '已关闭工单数',
                              work_order_close_rate       DECIMAL(5,2)    NOT NULL DEFAULT 0.00 COMMENT '工单闭环率，单位%',
                              avg_handle_minutes          DECIMAL(10,2)   NOT NULL DEFAULT 0.00 COMMENT '平均处理时长，单位分钟',
                              create_time                 DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                              update_time                 DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
                              PRIMARY KEY (id),
                              UNIQUE KEY uk_report_date (report_date)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci
    COMMENT = '运行日报表';

-- =========================================================
-- 初始化基础角色
-- =========================================================
INSERT INTO sys_role (id, role_code, role_name, description, status)
VALUES
    (1, 'admin',    '系统管理员', '拥有用户、设备、告警、工单、报表等全部权限', 1),
    (2, 'operator', '运维人员',   '可查看设备状态、确认告警、处理工单', 1),
    (3, 'viewer',   '只读用户',   '只能查看设备状态、告警记录和统计报表', 1);

-- =========================================================
-- 初始化用户
--
-- 默认密码均为：123456
-- 存储方式：SHA2('123456', 256)
--
-- Java 中可使用 Hutool 或 JDK MessageDigest 计算 SHA-256 后比对。
-- =========================================================
INSERT INTO sys_user (id, username, password, real_name, phone, email, status)
VALUES
    (1, 'admin',    SHA2('123456', 256), '系统管理员', '13800000001', 'admin@example.com', 1),
    (2, 'operator', SHA2('123456', 256), '运维人员',   '13800000002', 'operator@example.com', 1),
    (3, 'viewer',   SHA2('123456', 256), '只读用户',   '13800000003', 'viewer@example.com', 1);

-- =========================================================
-- 初始化用户角色关系
-- =========================================================
INSERT INTO sys_user_role (user_id, role_id)
VALUES
    (1, 1),
    (2, 2),
    (3, 3);

-- =========================================================
-- 初始化设备数据
-- =========================================================
INSERT INTO device_info (
    id,
    device_code,
    device_name,
    station_name,
    region,
    device_type,
    rated_power,
    longitude,
    latitude,
    online_status,
    running_status,
    last_heartbeat,
    manager_id,
    install_time,
    remark
)
VALUES
    (1, 'CP-0001', '一号直流快充桩', '城东充电站', '城东区', 'DC', 120.00, 118.796900, 32.060300, 1, 1, NOW(), 2, '2025-01-10 09:00:00', '示例设备：正常在线'),
    (2, 'CP-0002', '二号直流快充桩', '城东充电站', '城东区', 'DC', 120.00, 118.797100, 32.060500, 1, 1, NOW(), 2, '2025-01-12 09:00:00', '示例设备：正常在线'),
    (3, 'CP-0003', '三号交流慢充桩', '城西充电站', '城西区', 'AC',  7.00, 118.730000, 32.080000, 0, 1, NULL, 2, '2025-02-05 09:00:00', '示例设备：当前离线'),
    (4, 'CP-0004', '四号直流快充桩', '高新区充电站', '高新区', 'DC', 160.00, 118.880000, 32.020000, 1, 2, NOW(), 2, '2025-03-18 09:00:00', '示例设备：运行异常'),
    (5, 'CP-0005', '五号交流慢充桩', '江北充电站', '江北区', 'AC',  7.00, 118.760000, 32.110000, 1, 1, NOW(), 2, '2025-04-01 09:00:00', '示例设备：正常在线');

-- =========================================================
-- 初始化告警规则
--
-- MVP 阶段可以先不读取该表，而是在代码里写死规则。
-- 第二阶段再改为从 alarm_rule 动态读取。
-- =========================================================
INSERT INTO alarm_rule (
    id,
    rule_code,
    rule_name,
    alarm_type,
    device_type,
    metric_name,
    operator,
    threshold_value,
    window_size,
    trigger_count,
    alarm_level,
    enabled,
    remark
)
VALUES
    (1, 'RULE_TEMP_HIGH',      '设备高温告警',       'THRESHOLD',  NULL, 'temperature',   '>',  80.00, 1, 1, 3, 1, '温度超过80℃触发严重告警'),
    (2, 'RULE_VOLT_LOW',       '设备低电压告警',     'THRESHOLD',  NULL, 'voltage',       '<', 180.00, 1, 1, 2, 1, '电压低于180V触发重要告警'),
    (3, 'RULE_DELAY_HIGH',     '网络延迟过高告警',   'THRESHOLD',  NULL, 'network_delay', '>', 200.00, 1, 1, 1, 1, '网络延迟超过200ms触发一般告警'),
    (4, 'RULE_DEVICE_OFFLINE', '设备离线告警',       'OFFLINE',    NULL, 'heartbeat',     '>',  60.00, 1, 1, 2, 1, '超过60秒未上报心跳触发离线告警'),
    (5, 'RULE_TEMP_CONTINUE',  '连续高温告警',       'CONTINUOUS', NULL, 'temperature',   '>',  75.00, 5, 3, 3, 1, '最近5次中连续3次温度超过75℃触发告警');

-- =========================================================
-- 初始化少量运行数据
-- =========================================================
INSERT INTO device_data (
    device_id,
    device_code,
    voltage,
    current_value,
    power,
    temperature,
    soc,
    network_delay,
    fault_code,
    report_time
)
VALUES
    (1, 'CP-0001', 221.50, 32.40, 7.20, 45.20, 68.50,  80, 'NORMAL', NOW()),
    (2, 'CP-0002', 219.80, 30.10, 6.80, 48.60, 55.00,  95, 'NORMAL', NOW()),
    (4, 'CP-0004', 222.30, 40.20, 8.90, 82.60, 72.00, 120, 'TEMP_HIGH', NOW()),
    (5, 'CP-0005', 220.10, 16.30, 3.50, 39.40, 60.00,  70, 'NORMAL', NOW());

-- =========================================================
-- 初始化一条示例告警记录
-- =========================================================
INSERT INTO alarm_record (
    alarm_no,
    device_id,
    device_code,
    alarm_type,
    alarm_metric,
    alarm_level,
    current_value,
    threshold_value,
    alarm_message,
    alarm_status,
    first_time,
    last_time,
    alarm_count,
    work_order_generated,
    dedup_key
)
VALUES
    (
        CONCAT('AL', DATE_FORMAT(NOW(), '%Y%m%d%H%i%s'), '0001'),
        4,
        'CP-0004',
        'THRESHOLD',
        'temperature',
        3,
        82.60,
        80.00,
        '设备温度超过阈值，当前温度82.60℃，阈值80.00℃',
        0,
        NOW(),
        NOW(),
        1,
        0,
        'CP-0004:THRESHOLD:temperature'
    );