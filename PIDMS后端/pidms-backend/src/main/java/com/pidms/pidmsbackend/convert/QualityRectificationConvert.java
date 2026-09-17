package com.pidms.pidmsbackend.convert;

import com.pidms.pidmsbackend.dto.QualityRectificationDTO;
import com.pidms.pidmsbackend.entity.QualityRectification;
import com.pidms.pidmsbackend.vo.QualityRectificationVO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

/**
 * 质量整改单 转换器
 * <p>DTO→Entity 忽略审计字段（由 Service 补全）；Entity→VO 后 projectName 由 Service 联查补全。</p>
 */
@Mapper(componentModel = "spring")
public interface QualityRectificationConvert {

    @Mapping(target = "createTime", ignore = true)
    @Mapping(target = "updateTime", ignore = true)
    @Mapping(target = "createBy", ignore = true)
    @Mapping(target = "updateBy", ignore = true)
    QualityRectification dtoToEntity(QualityRectificationDTO dto);

    @Mapping(target = "projectName", ignore = true)
    QualityRectificationVO entityToVo(QualityRectification entity);

    List<QualityRectificationVO> entityListToVoList(List<QualityRectification> list);
}
