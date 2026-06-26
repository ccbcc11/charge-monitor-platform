package com.ccbcc.charge.monitor.module.auth.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

/**
 * 当前用户信息
 */
@Data
@Accessors(chain = true)
@Schema(description = "当前用户信息")
public class UserInfoVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

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
     * 手机号
     */
    @Schema(description = "手机号", example = "13800000001")
    private String phone;

    /**
     * 邮箱
     */
    @Schema(description = "邮箱", example = "admin@example.com")
    private String email;

    /**
     * 角色编码列表
     */
    @Schema(description = "角色编码列表", example = "[\"admin\"]")
    private List<String> roles;
}