package com.ccbcc.charge.monitor.common.result;

import com.ccbcc.charge.monitor.common.result.ResultCode;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 统一接口返回对象
 *
 * @param <T> 返回数据类型
 */
@Data
public class Result<T> implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 状态码
     */
    private Integer code;

    /**
     * 返回信息
     */
    private String message;

    /**
     * 返回数据
     */
    private T data;

    /**
     * 响应时间
     */
    private LocalDateTime timestamp;

    private Result() {
        this.timestamp = LocalDateTime.now();
    }

    private Result(Integer code, String message, T data) {
        this.code = code;
        this.message = message;
        this.data = data;
        this.timestamp = LocalDateTime.now();
    }

    public static <T> Result<T> success() {
        return new Result<>(
                ResultCode.SUCCESS.getCode(),
                ResultCode.SUCCESS.getMessage(),
                null
        );
    }

    public static <T> Result<T> success(T data) {
        return new Result<>(
                ResultCode.SUCCESS.getCode(),
                ResultCode.SUCCESS.getMessage(),
                data
        );
    }

    public static <T> Result<T> success(String message, T data) {
        return new Result<>(
                ResultCode.SUCCESS.getCode(),
                message,
                data
        );
    }

    public static <T> Result<T> fail() {
        return new Result<>(
                ResultCode.BUSINESS_ERROR.getCode(),
                ResultCode.BUSINESS_ERROR.getMessage(),
                null
        );
    }

    public static <T> Result<T> fail(String message) {
        return new Result<>(
                ResultCode.BUSINESS_ERROR.getCode(),
                message,
                null
        );
    }

    public static <T> Result<T> fail(ResultCode resultCode) {
        return new Result<>(
                resultCode.getCode(),
                resultCode.getMessage(),
                null
        );
    }

    public static <T> Result<T> fail(ResultCode resultCode, String message) {
        return new Result<>(
                resultCode.getCode(),
                message,
                null
        );
    }

    public static <T> Result<T> fail(Integer code, String message) {
        return new Result<>(
                code,
                message,
                null
        );
    }
}
