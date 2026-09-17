package com.pidms.pidmsbackend.convert;

import com.pidms.pidmsbackend.dto.BusinessTripDTO;
import com.pidms.pidmsbackend.entity.BusinessTrip;
import com.pidms.pidmsbackend.vo.BusinessTripVO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

/**
 * 出差申请 转换器
 * <p>DTO→Entity 忽略审计字段（由 Service 补全）。</p>
 */
@Mapper(componentModel = "spring")
public interface BusinessTripConvert {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createBy", ignore = true)
    @Mapping(target = "createTime", ignore = true)
    @Mapping(target = "updateBy", ignore = true)
    @Mapping(target = "updateTime", ignore = true)
    BusinessTrip dtoToEntity(BusinessTripDTO dto);

    @Mapping(target = "projectName", ignore = true)
    @Mapping(target = "tripDate", ignore = true)
    BusinessTripVO entityToVo(BusinessTrip entity);

    List<BusinessTripVO> entityListToVoList(List<BusinessTrip> list);
}
