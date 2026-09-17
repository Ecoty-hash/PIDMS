package com.pidms.pidmsbackend.convert;

import com.pidms.pidmsbackend.dto.RoleDTO;
import com.pidms.pidmsbackend.entity.SysRole;
import com.pidms.pidmsbackend.vo.RoleVO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

/**
 * 角色管理 转换器
 * <p>DTO→Entity 忽略主键与审计字段（由 Service 补全）。
 * 权限相关字段为二期预留，转换时忽略。</p>
 */
@Mapper(componentModel = "spring")
public interface RoleConvert {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createBy", ignore = true)
    @Mapping(target = "createTime", ignore = true)
    @Mapping(target = "updateBy", ignore = true)
    @Mapping(target = "updateTime", ignore = true)
    SysRole dtoToEntity(RoleDTO dto);

    @Mapping(target = "permissions", ignore = true)
    @Mapping(target = "userAssignments", ignore = true)
    @Mapping(target = "permissionSettings", ignore = true)
    RoleVO entityToVo(SysRole entity);

    List<RoleVO> entityListToVoList(List<SysRole> list);
}
