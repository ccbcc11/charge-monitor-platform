package com.ccbcc.charge.monitor.module.auth.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

/**
 * 登录响应结果
 */
@Data
@Accessors(chain = true)
@Schema(description = "登录响应结果")
public class LoginVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 登录 token
     */
    @Schema(description = "登录token", example = "8f72c7d0-76f9-4f5d-9a1d-2b7c16a0e111")
    private String token;

    /**
     * token 请求头名称
     */
    @Schema(description = "token请求头名称", example = "satoken")
    private String tokenName;

    /**
     * 用户ID
     */
    @Schema(description = "用户ID", example = "1")
    private Long userId;

    /**
     * 用户名
     */
    @Schema(description = "用户名", example = "admin")
    private String username;

    /**
     * 真实姓名
     */
    @Schema(description = "真实姓名", example = "系统管理员")
    private String realName;

    /**
     * 角色编码列表
     */
    @Schema(description = "角色编码列表", example = "[\"admin\"]")
    private List<String> roles;
}