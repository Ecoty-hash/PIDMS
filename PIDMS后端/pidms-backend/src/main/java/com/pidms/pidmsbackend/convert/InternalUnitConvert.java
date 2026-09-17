package com.pidms.pidmsbackend.convert;

import com.pidms.pidmsbackend.dto.InternalUnitDTO;
import com.pidms.pidmsbackend.entity.InternalUnit;
import com.pidms.pidmsbackend.vo.InternalUnitVO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

/**
 * 内部单位 转换器
 * <p>DTO→Entity 忽略审计字段（由 Service 补全）。</p>
 */
@Mapper(componentModel = "spring")
public interface InternalUnitConvert {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createBy", ignore = true)
    @Mapping(target = "createTime", ignore = true)
    @Mapping(target = "updateBy", ignore = true)
    @Mapping(target = "updateTime", ignore = true)
    InternalUnit dtoToEntity(InternalUnitDTO dto);

    InternalUnitVO entityToVo(InternalUnit entity);

    List<InternalUnitVO> entityListToVoList(List<InternalUnit> list);
}
