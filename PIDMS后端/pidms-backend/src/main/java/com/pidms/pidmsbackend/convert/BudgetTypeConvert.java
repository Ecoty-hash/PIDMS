package com.pidms.pidmsbackend.convert;

import com.pidms.pidmsbackend.dto.BudgetTypeDTO;
import com.pidms.pidmsbackend.entity.BudgetType;
import com.pidms.pidmsbackend.vo.BudgetTypeVO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

/**
 * 预算类型 转换器
 * <p>DTO→Entity 忽略审计字段（由 Service 补全）。</p>
 */
@Mapper(componentModel = "spring")
public interface BudgetTypeConvert {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createBy", ignore = true)
    @Mapping(target = "createTime", ignore = true)
    @Mapping(target = "updateBy", ignore = true)
    @Mapping(target = "updateTime", ignore = true)
    BudgetType dtoToEntity(BudgetTypeDTO dto);

    BudgetTypeVO entityToVo(BudgetType entity);

    List<BudgetTypeVO> entityListToVoList(List<BudgetType> list);
}
