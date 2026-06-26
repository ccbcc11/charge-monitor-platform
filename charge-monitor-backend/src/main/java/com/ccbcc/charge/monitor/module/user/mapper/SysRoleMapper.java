package com.ccbcc.charge.monitor.module.user.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ccbcc.charge.monitor.module.user.entity.SysRole;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 角色表 Mapper
 */
@Mapper
public interface SysRoleMapper extends BaseMapper<SysRole> {

    /**
     * 根据用户ID查询角色编码列表
     *
     * 用于登录成功后返回 roles，例如：admin / operator / viewer
     *
     * @param userId 用户ID
     * @return 角色编码列表
     */
    @Select("""
            SELECT r.role_code
            FROM sys_role r
            INNER JOIN sys_user_role ur ON r.id = ur.role_id
            WHERE ur.user_id = #{userId}
              AND r.status = 1
              AND r.deleted = 0
            """)
    List<String> selectRoleCodesByUserId(@Param("userId") Long userId);
}