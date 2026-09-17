package com.pidms.pidmsbackend.convert;

import com.pidms.pidmsbackend.dto.SafetyCheckItemDTO;
import com.pidms.pidmsbackend.entity.SafetyCheckItem;
import com.pidms.pidmsbackend.vo.SafetyCheckItemVO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

/**
 * 安全检查项 转换器
 */
@Mapper(componentModel = "spring")
public interface SafetyCheckItemConvert {

    @Mapping(target = "createTime", ignore = true)
    @Mapping(target = "updateTime", ignore = true)
    @Mapping(target = "createBy", ignore = true)
    @Mapping(target = "updateBy", ignore = true)
    SafetyCheckItem dtoToEntity(SafetyCheckItemDTO dto);

    SafetyCheckItemVO entityToVo(SafetyCheckItem entity);

    List<SafetyCheckItemVO> entityListToVoList(List<SafetyCheckItem> list);
}
