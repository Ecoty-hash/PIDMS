package com.pidms.pidmsbackend.convert;

import com.pidms.pidmsbackend.dto.SealTypeDTO;
import com.pidms.pidmsbackend.entity.SealType;
import com.pidms.pidmsbackend.vo.SealTypeVO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface SealTypeConvert {

    // 请求体 → 实体（审计字段由 Service 补全）
    @Mapping(target = "createTime", ignore = true)
    @Mapping(target = "updateTime", ignore = true)
    @Mapping(target = "createBy", ignore = true)
    @Mapping(target = "updateBy", ignore = true)
    SealType dtoToEntity(SealTypeDTO dto);

    SealTypeVO entityToVo(SealType entity);

    List<SealTypeVO> entityListToVoList(List<SealType> list);
}
