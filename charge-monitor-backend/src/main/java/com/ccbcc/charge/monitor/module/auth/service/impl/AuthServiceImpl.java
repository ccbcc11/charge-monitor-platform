package com.ccbcc.charge.monitor.module.auth.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.crypto.digest.DigestUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ccbcc.charge.monitor.common.exception.BusinessException;
import com.ccbcc.charge.monitor.common.result.ResultCode;
import com.ccbcc.charge.monitor.module.auth.dto.LoginDTO;
import com.ccbcc.charge.monitor.module.auth.service.AuthService;
import com.ccbcc.charge.monitor.module.auth.vo.LoginVO;
import com.ccbcc.charge.monitor.module.auth.vo.UserInfoVO;
import com.ccbcc.charge.monitor.module.user.entity.SysUser;
import com.ccbcc.charge.monitor.module.user.mapper.SysRoleMapper;
import com.ccbcc.charge.monitor.module.user.mapper.SysUserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;

/**
 * 认证服务实现类
 */
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final SysUserMapper sysUserMapper;
    private final SysRoleMapper sysRoleMapper;

    /**
     * 用户启用状态
     */
    private static final Integer USER_STATUS_ENABLED = 1;

    @Override
    public LoginVO login(LoginDTO loginDTO) {

        /*
         * 1. 根据用户名查询用户
         */
        SysUser user = sysUserMapper.selectOne(
                new LambdaQueryWrapper<SysUser>()
                        .eq(SysUser::getUsername, loginDTO.getUsername())
                        .last("LIMIT 1")
        );

        if (user == null) {
            throw new BusinessException(ResultCode.UNAUTHORIZED, "用户名或密码错误");
        }

        /*
         * 2. 判断用户状态
         */
        if (!Objects.equals(user.getStatus(), USER_STATUS_ENABLED)) {
            throw new BusinessException(ResultCode.FORBIDDEN, "当前用户已被禁用");
        }

        /*
         * 3. 校验密码
         *
         * init.sql 中使用的是：
         * SHA2('123456', 256)
         *
         * 所以后端这里用 Hutool 计算 SHA-256。
         */
        String inputPasswordSha256 = DigestUtil.sha256Hex(loginDTO.getPassword());

        if (!inputPasswordSha256.equalsIgnoreCase(user.getPassword())) {
            throw new BusinessException(ResultCode.UNAUTHORIZED, "用户名或密码错误");
        }

        /*
         * 4. 查询用户角色编码
         */
        List<String> roles = sysRoleMapper.selectRoleCodesByUserId(user.getId());

        /*
         * 5. 调用 Sa-Token 登录
         */
        StpUtil.login(user.getId());

        /*
         * 6. 获取 token
         */
        String tokenValue = StpUtil.getTokenValue();
        String tokenName = StpUtil.getTokenName();

        /*
         * 7. 返回登录结果
         */
        return new LoginVO()
                .setToken(tokenValue)
                .setTokenName(tokenName)
                .setUserId(user.getId())
                .setUsername(user.getUsername())
                .setRealName(user.getRealName())
                .setRoles(roles);
    }

    @Override
    public UserInfoVO userInfo() {

        /*
         * 1. 获取当前登录用户ID
         */
        Long userId = StpUtil.getLoginIdAsLong();

        /*
         * 2. 查询用户信息
         */
        SysUser user = sysUserMapper.selectById(userId);

        if (user == null) {
            throw new BusinessException(ResultCode.UNAUTHORIZED, "当前登录用户不存在");
        }

        /*
         * 3. 查询角色编码
         */
        List<String> roles = sysRoleMapper.selectRoleCodesByUserId(userId);

        /*
         * 4. 返回当前用户信息
         */
        return new UserInfoVO()
                .setUserId(user.getId())
                .setUsername(user.getUsername())
                .setRealName(user.getRealName())
                .setPhone(user.getPhone())
                .setEmail(user.getEmail())
                .setRoles(roles);
    }

    @Override
    public void logout() {

        /*
         * 退出登录前先判断是否登录。
         * 如果没有登录，Sa-Token 会抛出 NotLoginException，
         * 由 GlobalExceptionHandler 统一返回 401。
         */
        StpUtil.checkLogin();

        StpUtil.logout();
    }
}