package com.pidms.pidmsbackend.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.pidms.pidmsbackend.entity.SysUser;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface SysUserMapper extends BaseMapper<SysUser> {

    /** 查询用户第一个角色的名称（如"管理员"），用于登录返回 */
    @Select("SELECT r.role_name FROM sys_user_role ur LEFT JOIN sys_role r ON ur.role_id = r.id WHERE ur.user_id = #{userId} LIMIT 1")
    String selectRoleName(Long userId);
}
