package com.pidms.pidmsbackend.convert;

import com.pidms.pidmsbackend.dto.QualityCheckItemDTO;
import com.pidms.pidmsbackend.entity.QualityCheckItem;
import com.pidms.pidmsbackend.vo.QualityCheckItemVO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

/**
 * 质量检查项 转换器
 */
@Mapper(componentModel = "spring")
public interface QualityCheckItemConvert {

    @Mapping(target = "createTime", ignore = true)
    @Mapping(target = "updateTime", ignore = true)
    @Mapping(target = "createBy", ignore = true)
    @Mapping(target = "updateBy", ignore = true)
    QualityCheckItem dtoToEntity(QualityCheckItemDTO dto);

    QualityCheckItemVO entityToVo(QualityCheckItem entity);

    List<QualityCheckItemVO> entityListToVoList(List<QualityCheckItem> list);
}
