package com.pidms.pidmsbackend.convert;

import com.pidms.pidmsbackend.dto.WarningRuleDTO;
import com.pidms.pidmsbackend.entity.WarningRule;
import com.pidms.pidmsbackend.vo.WarningRuleVO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

/**
 * 预警规则 转换器
 * <p>DTO→Entity 忽略审计字段（由 Service 补全）；Entity→VO 后 projectName 由 Service 联查补全。</p>
 */
@Mapper(componentModel = "spring")
public interface WarningRuleConvert {

    @Mapping(target = "createTime", ignore = true)
    @Mapping(target = "updateTime", ignore = true)
    @Mapping(target = "createBy", ignore = true)
    @Mapping(target = "updateBy", ignore = true)
    WarningRule dtoToEntity(WarningRuleDTO dto);

    @Mapping(target = "projectName", ignore = true)
    WarningRuleVO entityToVo(WarningRule entity);

    List<WarningRuleVO> entityListToVoList(List<WarningRule> list);
}
