package com.ccbcc.charge.monitor.module.auth.service;

import com.ccbcc.charge.monitor.module.auth.dto.LoginDTO;
import com.ccbcc.charge.monitor.module.auth.vo.LoginVO;
import com.ccbcc.charge.monitor.module.auth.vo.UserInfoVO;

/**
 * 认证服务接口
 */
public interface AuthService {

    /**
     * 用户登录
     *
     * @param loginDTO 登录请求参数
     * @return 登录结果
     */
    LoginVO login(LoginDTO loginDTO);

    /**
     * 获取当前登录用户信息
     *
     * @return 当前用户信息
     */
    UserInfoVO userInfo();

    /**
     * 用户退出登录
     */
    void logout();
}