package com.ccbcc.charge.monitor.module.user.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ccbcc.charge.monitor.module.user.entity.SysUserRole;
import org.apache.ibatis.annotations.Mapper;

/**
 * 用户角色关联表 Mapper
 */
@Mapper
public interface SysUserRoleMapper extends BaseMapper<SysUserRole> {
}