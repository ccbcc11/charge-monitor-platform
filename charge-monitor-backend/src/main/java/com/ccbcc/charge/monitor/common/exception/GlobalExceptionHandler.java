package com.ccbcc.charge.monitor.common.exception;

import cn.dev33.satoken.exception.NotLoginException;
import cn.dev33.satoken.exception.NotPermissionException;
import com.ccbcc.charge.monitor.common.exception.BusinessException;
import com.ccbcc.charge.monitor.common.result.Result;
import com.ccbcc.charge.monitor.common.result.ResultCode;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.stream.Collectors;

/**
 * 全局异常处理器
 *
 * 作用：
 * 1. 统一捕获 Controller 层抛出的异常
 * 2. 统一返回 Result 格式
 * 3. 避免把 Java 异常堆栈直接暴露给前端
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * 业务异常
     */
    @ExceptionHandler(BusinessException.class)
    public Result<Void> handleBusinessException(BusinessException e) {
        log.warn("业务异常：{}", e.getMessage());
        return Result.fail(e.getCode(), e.getMessage());
    }

    /**
     * Sa-Token 未登录异常
     */
    @ExceptionHandler(NotLoginException.class)
    public Result<Void> handleNotLoginException(NotLoginException e) {
        log.warn("用户未登录或登录已过期：{}", e.getMessage());
        return Result.fail(ResultCode.UNAUTHORIZED);
    }

    /**
     * Sa-Token 无权限异常
     */
    @ExceptionHandler(NotPermissionException.class)
    public Result<Void> handleNotPermissionException(NotPermissionException e) {
        log.warn("无权限访问：{}", e.getMessage());
        return Result.fail(ResultCode.FORBIDDEN);
    }

    /**
     * @RequestBody 参数校验异常
     *
     * 例如：
     * @Valid @RequestBody DeviceCreateDTO dto
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public Result<Void> handleMethodArgumentNotValidException(MethodArgumentNotValidException e) {
        FieldError fieldError = e.getBindingResult().getFieldError();

        String message = fieldError == null
                ? ResultCode.PARAM_ERROR.getMessage()
                : fieldError.getDefaultMessage();

        log.warn("请求体参数校验失败：{}", message);
        return Result.fail(ResultCode.PARAM_ERROR, message);
    }

    /**
     * @RequestParam / @PathVariable 参数校验异常
     *
     * 例如：
     * @NotBlank String deviceCode
     */
    @ExceptionHandler(ConstraintViolationException.class)
    public Result<Void> handleConstraintViolationException(ConstraintViolationException e) {
        String message = e.getConstraintViolations()
                .stream()
                .map(ConstraintViolation::getMessage)
                .collect(Collectors.joining("; "));

        log.warn("请求参数校验失败：{}", message);
        return Result.fail(ResultCode.PARAM_ERROR, message);
    }

    /**
     * 缺少必要请求参数
     */
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public Result<Void> handleMissingServletRequestParameterException(MissingServletRequestParameterException e) {
        String message = "缺少必要请求参数：" + e.getParameterName();
        log.warn(message);
        return Result.fail(ResultCode.PARAM_ERROR, message);
    }

    /**
     * 参数类型转换异常
     *
     * 例如：接口需要 Long id，但传入 abc
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public Result<Void> handleMethodArgumentTypeMismatchException(MethodArgumentTypeMismatchException e) {
        String message = "参数类型错误：" + e.getName();
        log.warn(message);
        return Result.fail(ResultCode.PARAM_ERROR, message);
    }

    /**
     * JSON 格式错误
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public Result<Void> handleHttpMessageNotReadableException(HttpMessageNotReadableException e) {
        log.warn("请求体JSON格式错误：{}", e.getMessage());
        return Result.fail(ResultCode.PARAM_ERROR, "请求体JSON格式错误");
    }

    /**
     * 请求方法不支持
     *
     * 例如：接口只支持 POST，却使用 GET 请求
     */
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public Result<Void> handleHttpRequestMethodNotSupportedException(HttpRequestMethodNotSupportedException e) {
        String message = "请求方法不支持：" + e.getMethod();
        log.warn(message);
        return Result.fail(ResultCode.METHOD_NOT_ALLOWED, message);
    }

    /**
     * 数据库唯一索引、非空约束等异常
     *
     * 例如：device_code 重复
     */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public Result<Void> handleDataIntegrityViolationException(DataIntegrityViolationException e) {
        log.error("数据库约束异常：", e);
        return Result.fail(ResultCode.DATABASE_ERROR, "数据库操作失败，可能存在重复数据或必填字段为空");
    }

    /**
     * 兜底异常
     *
     * 所有没有被上面捕获的异常，都会进入这里。
     */
    @ExceptionHandler(Exception.class)
    public Result<Void> handleException(Exception e) {
        log.error("系统异常：", e);
        return Result.fail(ResultCode.SYSTEM_ERROR, "系统繁忙，请稍后重试");
    }
}
