-- =========================================================
-- 新能源充电设施运行监测与智能告警平台
-- 建表脚本：schema.sql
-- =========================================================

CREATE DATABASE IF NOT EXISTS charge_monitor
    DEFAULT CHARACTER SET utf8mb4
    COLLATE utf8mb4_0900_ai_ci;

USE charge_monitor;

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

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
    password        VARCHAR(128)    NOT NULL COMMENT '密码，SHA-256 存储',
    real_name       VARCHAR(50)     DEFAULT NULL COMMENT '真实姓名',
    phone           VARCHAR(20)     DEFAULT NULL COMMENT '手机号',
    email           VARCHAR(100)    DEFAULT NULL COMMENT '邮箱',
    status          TINYINT         NOT NULL DEFAULT 1 COMMENT '状态：0禁用，1启用',
    create_time     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted         TINYINT         NOT NULL DEFAULT 0 COMMENT '逻辑删除',
    PRIMARY KEY (id),
    UNIQUE KEY uk_username (username)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '用户表';

-- =========================================================
-- 2. 角色表
-- =========================================================
CREATE TABLE sys_role (
    id              BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    role_code       VARCHAR(50)     NOT NULL COMMENT '角色编码',
    role_name       VARCHAR(50)     NOT NULL COMMENT '角色名称',
    description     VARCHAR(255)    DEFAULT NULL COMMENT '角色描述',
    status          TINYINT         NOT NULL DEFAULT 1 COMMENT '状态',
    create_time     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted         TINYINT         NOT NULL DEFAULT 0 COMMENT '逻辑删除',
    PRIMARY KEY (id),
    UNIQUE KEY uk_role_code (role_code)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '角色表';

-- =========================================================
-- 3. 用户角色关联表
-- =========================================================
CREATE TABLE sys_user_role (
    id              BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    user_id         BIGINT UNSIGNED NOT NULL COMMENT '用户ID',
    role_id         BIGINT UNSIGNED NOT NULL COMMENT '角色ID',
    create_time     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_user_role (user_id, role_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '用户角色关联表';

-- =========================================================
-- 4. 设备信息表
-- =========================================================
CREATE TABLE device_info (
    id              BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    device_code     VARCHAR(64)     NOT NULL COMMENT '设备编号',
    device_name     VARCHAR(100)    NOT NULL COMMENT '设备名称',
    station_name    VARCHAR(100)    DEFAULT NULL COMMENT '所属站点',
    region          VARCHAR(50)     DEFAULT NULL COMMENT '所属区域',
    device_type     VARCHAR(50)     DEFAULT NULL COMMENT '设备类型：AC/DC',
    rated_power     DECIMAL(10,2)   DEFAULT NULL COMMENT '额定功率 kW',
    longitude       DECIMAL(10,6)   DEFAULT NULL COMMENT '经度',
    latitude        DECIMAL(10,6)   DEFAULT NULL COMMENT '纬度',
    online_status   TINYINT         NOT NULL DEFAULT 0 COMMENT '在线状态：0离线 1在线',
    running_status  TINYINT         NOT NULL DEFAULT 1 COMMENT '运行状态：0停用 1正常 2异常',
    last_heartbeat  DATETIME        DEFAULT NULL COMMENT '最近心跳时间',
    manager_id      BIGINT UNSIGNED DEFAULT NULL COMMENT '负责人ID',
    install_time    DATETIME        DEFAULT NULL COMMENT '投运时间',
    remark          VARCHAR(255)    DEFAULT NULL COMMENT '备注',
    create_time     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted         TINYINT         NOT NULL DEFAULT 0 COMMENT '逻辑删除',
    PRIMARY KEY (id),
    UNIQUE KEY uk_device_code (device_code),
    KEY idx_region (region),
    KEY idx_online_status (online_status),
    KEY idx_last_heartbeat (last_heartbeat)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '设备信息表';

-- =========================================================
-- 5. 设备运行数据表
-- =========================================================
CREATE TABLE device_data (
    id              BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    device_id       BIGINT UNSIGNED NOT NULL COMMENT '设备ID',
    device_code     VARCHAR(64)     NOT NULL COMMENT '设备编号',
    voltage         DECIMAL(10,2)   DEFAULT NULL COMMENT '电压 V',
    current_value   DECIMAL(10,2)   DEFAULT NULL COMMENT '电流 A',
    power           DECIMAL(10,2)   DEFAULT NULL COMMENT '功率 kW',
    temperature     DECIMAL(10,2)   DEFAULT NULL COMMENT '温度 ℃',
    soc             DECIMAL(5,2)    DEFAULT NULL COMMENT 'SOC %',
    network_delay   INT             DEFAULT NULL COMMENT '网络延迟 ms',
    fault_code      VARCHAR(50)     DEFAULT 'NORMAL' COMMENT '故障码',
    report_time     DATETIME        NOT NULL COMMENT '设备上报时间',
    create_time     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '入库时间',
    PRIMARY KEY (id),
    KEY idx_code_time (device_code, report_time),
    KEY idx_report_time (report_time)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '设备运行数据表';

-- =========================================================
-- 6. 告警规则表
-- =========================================================
CREATE TABLE alarm_rule (
    id              BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    rule_code       VARCHAR(64)     NOT NULL COMMENT '规则编码',
    rule_name       VARCHAR(100)    NOT NULL COMMENT '规则名称',
    alarm_type      VARCHAR(50)     NOT NULL COMMENT '告警类型',
    device_type     VARCHAR(50)     DEFAULT NULL COMMENT '适用设备类型',
    metric_name     VARCHAR(50)     NOT NULL COMMENT '指标名称',
    operator        VARCHAR(10)     NOT NULL COMMENT '运算符',
    threshold_value DECIMAL(10,2)   DEFAULT NULL COMMENT '阈值',
    window_size     INT             NOT NULL DEFAULT 1 COMMENT '窗口大小',
    trigger_count   INT             NOT NULL DEFAULT 1 COMMENT '触发次数',
    alarm_level     TINYINT         NOT NULL DEFAULT 1 COMMENT '告警等级',
    enabled         TINYINT         NOT NULL DEFAULT 1 COMMENT '是否启用',
    remark          VARCHAR(255)    DEFAULT NULL COMMENT '备注',
    create_time     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted         TINYINT         NOT NULL DEFAULT 0 COMMENT '逻辑删除',
    PRIMARY KEY (id),
    UNIQUE KEY uk_rule_code (rule_code),
    KEY idx_enabled (enabled),
    KEY idx_alarm_type (alarm_type)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '告警规则表';

-- =========================================================
-- 7. 告警记录表
-- =========================================================
CREATE TABLE alarm_record (
    id                  BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    alarm_no            VARCHAR(64)     NOT NULL COMMENT '告警编号',
    device_id           BIGINT UNSIGNED NOT NULL COMMENT '设备ID',
    device_code         VARCHAR(64)     NOT NULL COMMENT '设备编号',
    alarm_type          VARCHAR(50)     NOT NULL COMMENT '告警类型',
    alarm_metric        VARCHAR(50)     NOT NULL COMMENT '告警指标',
    alarm_level         TINYINT         NOT NULL DEFAULT 1 COMMENT '告警等级',
    current_value       DECIMAL(10,2)   DEFAULT NULL COMMENT '当前值',
    threshold_value     DECIMAL(10,2)   DEFAULT NULL COMMENT '阈值',
    alarm_message       VARCHAR(255)    NOT NULL COMMENT '告警描述',
    alarm_status        TINYINT         NOT NULL DEFAULT 0 COMMENT '告警状态',
    first_time          DATETIME        NOT NULL COMMENT '首次发生时间',
    last_time           DATETIME        NOT NULL COMMENT '最近发生时间',
    recover_time        DATETIME        DEFAULT NULL COMMENT '恢复时间',
    alarm_count         INT             NOT NULL DEFAULT 1 COMMENT '发生次数',
    work_order_generated TINYINT         NOT NULL DEFAULT 0 COMMENT '是否已生成工单',
    dedup_key           VARCHAR(200)    DEFAULT NULL COMMENT '去重键',
    create_time         DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time         DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted             TINYINT         NOT NULL DEFAULT 0 COMMENT '逻辑删除',
    PRIMARY KEY (id),
    UNIQUE KEY uk_alarm_no (alarm_no),
    KEY idx_device_status (device_id, alarm_status),
    KEY idx_code_status (device_code, alarm_status),
    KEY idx_alarm_level (alarm_level),
    KEY idx_alarm_type (alarm_type),
    KEY idx_dedup_key (dedup_key)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '告警记录表';

-- =========================================================
-- 8. 运维工单表
-- =========================================================
CREATE TABLE work_order (
    id              BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    work_order_no   VARCHAR(64)     NOT NULL COMMENT '工单编号',
    alarm_id        BIGINT UNSIGNED NOT NULL COMMENT '关联告警ID',
    device_id       BIGINT UNSIGNED NOT NULL COMMENT '设备ID',
    device_code     VARCHAR(64)     NOT NULL COMMENT '设备编号',
    title           VARCHAR(200)    NOT NULL COMMENT '工单标题',
    content         VARCHAR(1000)   DEFAULT NULL COMMENT '工单内容',
    priority        TINYINT         NOT NULL DEFAULT 2 COMMENT '优先级',
    status          TINYINT         NOT NULL DEFAULT 0 COMMENT '工单状态',
    assignee_id     BIGINT UNSIGNED DEFAULT NULL COMMENT '处理人ID',
    assignee_name   VARCHAR(100)    DEFAULT NULL COMMENT '处理人名称',
    finish_time     DATETIME        DEFAULT NULL COMMENT '完成时间',
    remark          VARCHAR(255)    DEFAULT NULL COMMENT '备注',
    create_time     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted         TINYINT         NOT NULL DEFAULT 0 COMMENT '逻辑删除',
    PRIMARY KEY (id),
    UNIQUE KEY uk_work_order_no (work_order_no),
    UNIQUE KEY uk_alarm_id (alarm_id),
    KEY idx_device_code (device_code),
    KEY idx_status (status),
    KEY idx_priority (priority)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '运维工单表';
