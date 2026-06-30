-- 运维工单表
-- 请在 IDEA 数据库工具或 Navicat 中执行

CREATE TABLE IF NOT EXISTS work_order (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    work_order_no VARCHAR(64) NOT NULL COMMENT '工单编号',
    alarm_id BIGINT UNSIGNED NOT NULL COMMENT '关联告警ID',
    device_id BIGINT UNSIGNED NOT NULL COMMENT '设备ID',
    device_code VARCHAR(64) NOT NULL COMMENT '设备编号',
    title VARCHAR(200) NOT NULL COMMENT '工单标题',
    content VARCHAR(1000) DEFAULT NULL COMMENT '工单内容',
    priority TINYINT NOT NULL DEFAULT 2 COMMENT '优先级：1低，2中，3高',
    status TINYINT NOT NULL DEFAULT 0 COMMENT '工单状态：0待处理，1处理中，2已完成，3已关闭',
    assignee_id BIGINT UNSIGNED DEFAULT NULL COMMENT '处理人ID',
    assignee_name VARCHAR(100) DEFAULT NULL COMMENT '处理人名称',
    finish_time DATETIME DEFAULT NULL COMMENT '完成时间',
    remark VARCHAR(255) DEFAULT NULL COMMENT '备注',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0未删除，1已删除',
    PRIMARY KEY (id),
    UNIQUE KEY uk_work_order_no (work_order_no),
    UNIQUE KEY uk_alarm_id (alarm_id),
    KEY idx_device_code (device_code),
    KEY idx_status (status),
    KEY idx_priority (priority),
    KEY idx_create_time (create_time)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci
  COMMENT = '运维工单表';
