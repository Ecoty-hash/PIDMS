package com.pidms.pidmsbackend.convert;

import com.pidms.pidmsbackend.dto.ProjectStatusDTO;
import com.pidms.pidmsbackend.entity.ProjectStatus;
import com.pidms.pidmsbackend.vo.ProjectStatusVO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface ProjectStatusConvert {

    // 请求体 → 实体（projectId 等业务外键/审计字段由 Service 补全）
    @Mapping(target = "createTime", ignore = true)
    @Mapping(target = "updateTime", ignore = true)
    @Mapping(target = "createBy", ignore = true)
    @Mapping(target = "updateBy", ignore = true)
    ProjectStatus dtoToEntity(ProjectStatusDTO dto);

    ProjectStatusVO entityToVo(ProjectStatus entity);

    List<ProjectStatusVO> entityListToVoList(List<ProjectStatus> list);
}
