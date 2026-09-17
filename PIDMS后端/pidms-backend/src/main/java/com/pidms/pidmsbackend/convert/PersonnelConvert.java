package com.pidms.pidmsbackend.convert;

import com.pidms.pidmsbackend.dto.PersonnelDTO;
import com.pidms.pidmsbackend.entity.SysUser;
import com.pidms.pidmsbackend.vo.PersonnelVO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

/**
 * 人员管理 转换器
 * <p>DTO→Entity 忽略主键、登录账号密码与审计字段（由 Service 补全）。</p>
 */
@Mapper(componentModel = "spring")
public interface PersonnelConvert {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "username", ignore = true)
    @Mapping(target = "password", ignore = true)
    @Mapping(target = "createBy", ignore = true)
    @Mapping(target = "createTime", ignore = true)
    @Mapping(target = "updateBy", ignore = true)
    @Mapping(target = "updateTime", ignore = true)
    SysUser dtoToEntity(PersonnelDTO dto);

    @Mapping(target = "roleId", ignore = true)
    @Mapping(target = "roleName", ignore = true)
    @Mapping(target = "orgName", ignore = true)
    PersonnelVO entityToVo(SysUser entity);

    List<PersonnelVO> entityListToVoList(List<SysUser> list);
}
