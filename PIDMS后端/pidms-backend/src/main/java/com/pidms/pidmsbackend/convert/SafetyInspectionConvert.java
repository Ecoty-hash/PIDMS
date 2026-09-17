package com.pidms.pidmsbackend.convert;

import com.pidms.pidmsbackend.dto.SafetyInspectionDTO;
import com.pidms.pidmsbackend.entity.SafetyInspection;
import com.pidms.pidmsbackend.vo.SafetyInspectionVO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

/**
 * 安全检查 转换器
 * <p>DTO→Entity 忽略审计字段（由 Service 补全）；Entity→VO 后 projectName 由 Service 联查补全。</p>
 */
@Mapper(componentModel = "spring")
public interface SafetyInspectionConvert {

    @Mapping(target = "createTime", ignore = true)
    @Mapping(target = "updateTime", ignore = true)
    @Mapping(target = "createBy", ignore = true)
    @Mapping(target = "updateBy", ignore = true)
    SafetyInspection dtoToEntity(SafetyInspectionDTO dto);

    @Mapping(target = "projectName", ignore = true)
    SafetyInspectionVO entityToVo(SafetyInspection entity);

    List<SafetyInspectionVO> entityListToVoList(List<SafetyInspection> list);
}
