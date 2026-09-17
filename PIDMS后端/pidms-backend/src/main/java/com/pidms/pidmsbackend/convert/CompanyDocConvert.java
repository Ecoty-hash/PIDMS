package com.pidms.pidmsbackend.convert;

import com.pidms.pidmsbackend.dto.CompanyDocDTO;
import com.pidms.pidmsbackend.entity.CompanyDoc;
import com.pidms.pidmsbackend.vo.CompanyDocVO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

/**
 * 公司文档 转换器
 * <p>DTO→Entity 忽略审计字段（由 Service 补全）。</p>
 */
@Mapper(componentModel = "spring")
public interface CompanyDocConvert {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createBy", ignore = true)
    @Mapping(target = "createTime", ignore = true)
    @Mapping(target = "updateBy", ignore = true)
    @Mapping(target = "updateTime", ignore = true)
    CompanyDoc dtoToEntity(CompanyDocDTO dto);

    CompanyDocVO entityToVo(CompanyDoc entity);

    List<CompanyDocVO> entityListToVoList(List<CompanyDoc> list);
}
