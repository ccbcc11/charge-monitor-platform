package com.ccbcc.charge.monitor.common.exception;

import com.ccbcc.charge.monitor.common.result.ResultCode;
import lombok.Getter;

/**
 * 业务异常
 *
 * 用于主动抛出可预期的业务错误，例如：
 * 1. 设备不存在
 * 2. 用户名或密码错误
 * 3. 告警记录不存在
 * 4. 工单状态不允许流转
 */
@Getter
public class BusinessException extends RuntimeException {

    private final Integer code;

    public BusinessException(String message) {
        super(message);
        this.code = ResultCode.BUSINESS_ERROR.getCode();
    }

    public BusinessException(ResultCode resultCode) {
        super(resultCode.getMessage());
        this.code = resultCode.getCode();
    }

    public BusinessException(ResultCode resultCode, String message) {
        super(message);
        this.code = resultCode.getCode();
    }

    public BusinessException(Integer code, String message) {
        super(message);
        this.code = code;
    }

    public BusinessException(ResultCode resultCode, Throwable cause) {
        super(resultCode.getMessage(), cause);
        this.code = resultCode.getCode();
    }

    public BusinessException(String message, Throwable cause) {
        super(message, cause);
        this.code = ResultCode.BUSINESS_ERROR.getCode();
    }
}
