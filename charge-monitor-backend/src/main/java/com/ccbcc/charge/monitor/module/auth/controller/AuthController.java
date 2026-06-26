package com.ccbcc.charge.monitor.module.auth.controller;

import com.ccbcc.charge.monitor.common.result.Result;
import com.ccbcc.charge.monitor.module.auth.dto.LoginDTO;
import com.ccbcc.charge.monitor.module.auth.service.AuthService;
import com.ccbcc.charge.monitor.module.auth.vo.LoginVO;
import com.ccbcc.charge.monitor.module.auth.vo.UserInfoVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 认证接口
 */
@Tag(name = "认证模块")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    /**
     * 用户登录
     */
    @Operation(summary = "用户登录")
    @PostMapping("/login")
    public Result<LoginVO> login(@Valid @RequestBody LoginDTO loginDTO) {
        return Result.success(authService.login(loginDTO));
    }

    /**
     * 获取当前用户信息
     */
    @Operation(summary = "获取当前用户信息")
    @GetMapping("/userInfo")
    public Result<UserInfoVO> userInfo() {
        return Result.success(authService.userInfo());
    }

    /**
     * 用户退出登录
     */
    @Operation(summary = "用户退出登录")
    @PostMapping("/logout")
    public Result<Void> logout() {
        authService.logout();
        return Result.success();
    }
}