package com.pidms.pidmsbackend.convert;

import com.pidms.pidmsbackend.dto.SafetyRectificationDTO;
import com.pidms.pidmsbackend.entity.SafetyRectification;
import com.pidms.pidmsbackend.vo.SafetyRectificationVO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

/**
 * 安全整改单 转换器
 * <p>DTO→Entity 忽略审计字段（由 Service 补全）；Entity→VO 后 projectName 由 Service 联查补全。</p>
 */
@Mapper(componentModel = "spring")
public interface SafetyRectificationConvert {

    @Mapping(target = "createTime", ignore = true)
    @Mapping(target = "updateTime", ignore = true)
    @Mapping(target = "createBy", ignore = true)
    @Mapping(target = "updateBy", ignore = true)
    SafetyRectification dtoToEntity(SafetyRectificationDTO dto);

    @Mapping(target = "projectName", ignore = true)
    SafetyRectificationVO entityToVo(SafetyRectification entity);

    List<SafetyRectificationVO> entityListToVoList(List<SafetyRectification> list);
}
