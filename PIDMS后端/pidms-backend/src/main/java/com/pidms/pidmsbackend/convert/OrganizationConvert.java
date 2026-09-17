package com.pidms.pidmsbackend.convert;

import com.pidms.pidmsbackend.dto.OrganizationDTO;
import com.pidms.pidmsbackend.entity.SysOrg;
import com.pidms.pidmsbackend.vo.OrganizationVO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

/**
 * 机构管理 转换器
 * <p>出入参用 parentOrgId（与接口文档一致），落库列为 sys_org.parent_id，此处显式映射。</p>
 */
@Mapper(componentModel = "spring")
public interface OrganizationConvert {

    @Mapping(target = "id", ignore = true)
    @Mapping(source = "parentOrgId", target = "parentId")
    @Mapping(target = "createBy", ignore = true)
    @Mapping(target = "createTime", ignore = true)
    @Mapping(target = "updateBy", ignore = true)
    @Mapping(target = "updateTime", ignore = true)
    SysOrg dtoToEntity(OrganizationDTO dto);

    @Mapping(source = "parentId", target = "parentOrgId")
    @Mapping(target = "parentOrgName", ignore = true)
    @Mapping(target = "children", ignore = true)
    OrganizationVO entityToVo(SysOrg entity);

    List<OrganizationVO> entityListToVoList(List<SysOrg> list);
}
