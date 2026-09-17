package com.pidms.pidmsbackend.convert;

import com.pidms.pidmsbackend.dto.WorkTypeDTO;
import com.pidms.pidmsbackend.entity.WorkType;
import com.pidms.pidmsbackend.vo.WorkTypeVO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

/**
 * 工种类型 转换器
 * <p>DTO→Entity 忽略审计字段（由 Service 补全）。</p>
 */
@Mapper(componentModel = "spring")
public interface WorkTypeConvert {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createBy", ignore = true)
    @Mapping(target = "createTime", ignore = true)
    @Mapping(target = "updateBy", ignore = true)
    @Mapping(target = "updateTime", ignore = true)
    WorkType dtoToEntity(WorkTypeDTO dto);

    WorkTypeVO entityToVo(WorkType entity);

    List<WorkTypeVO> entityListToVoList(List<WorkType> list);
}
