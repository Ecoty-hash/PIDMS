package com.pidms.pidmsbackend.convert;

import com.pidms.pidmsbackend.dto.QualityInspectionDTO;
import com.pidms.pidmsbackend.entity.QualityInspection;
import com.pidms.pidmsbackend.vo.QualityInspectionVO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

/**
 * 质量检查 转换器
 * <p>DTO→Entity 忽略审计字段（由 Service 补全）；Entity→VO 后 projectName 由 Service 联查补全。</p>
 */
@Mapper(componentModel = "spring")
public interface QualityInspectionConvert {

    @Mapping(target = "createTime", ignore = true)
    @Mapping(target = "updateTime", ignore = true)
    @Mapping(target = "createBy", ignore = true)
    @Mapping(target = "updateBy", ignore = true)
    QualityInspection dtoToEntity(QualityInspectionDTO dto);

    @Mapping(target = "projectName", ignore = true)
    QualityInspectionVO entityToVo(QualityInspection entity);

    List<QualityInspectionVO> entityListToVoList(List<QualityInspection> list);
}
