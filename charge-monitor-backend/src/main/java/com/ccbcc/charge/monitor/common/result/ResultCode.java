package com.ccbcc.charge.monitor.common.result;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 统一响应状态码
 */
@Getter
@AllArgsConstructor
public enum ResultCode {

    SUCCESS(200, "操作成功"),

    PARAM_ERROR(400, "请求参数错误"),
    UNAUTHORIZED(401, "用户未登录或登录已过期"),
    FORBIDDEN(403, "无权限访问"),
    NOT_FOUND(404, "请求资源不存在"),
    METHOD_NOT_ALLOWED(405, "请求方法不支持"),

    BUSINESS_ERROR(50001, "业务处理失败"),
    DATA_NOT_FOUND(50002, "数据不存在"),
    DATA_ALREADY_EXISTS(50003, "数据已存在"),

    DEVICE_NOT_FOUND(51001, "设备不存在"),
    DEVICE_DISABLED(51002, "设备已停用"),
    DEVICE_OFFLINE(51003, "设备离线"),

    ALARM_ALREADY_EXISTS(52001, "告警已存在"),
    ALARM_NOT_FOUND(52002, "告警不存在"),
    ALARM_RULE_NOT_FOUND(52003, "告警规则不存在"),
    ALARM_RULE_CODE_EXISTS(52004, "告警规则编码已存在"),

    WORK_ORDER_NOT_FOUND(53001, "工单不存在"),
    WORK_ORDER_STATUS_ERROR(53002, "工单状态不正确"),

    DATABASE_ERROR(54001, "数据库操作异常"),
    REDIS_ERROR(54002, "Redis操作异常"),

    SYSTEM_ERROR(500, "系统异常");

    private final Integer code;
    private final String message;
}
