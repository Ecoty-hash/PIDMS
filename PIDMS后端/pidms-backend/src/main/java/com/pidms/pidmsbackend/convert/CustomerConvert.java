package com.pidms.pidmsbackend.convert;

import com.pidms.pidmsbackend.dto.CustomerDTO;
import com.pidms.pidmsbackend.entity.Customer;
import com.pidms.pidmsbackend.vo.CustomerVO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

/**
 * 客户 转换器
 * <p>DTO→Entity 忽略审计字段（由 Service 补全）。</p>
 */
@Mapper(componentModel = "spring")
public interface CustomerConvert {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createBy", ignore = true)
    @Mapping(target = "createTime", ignore = true)
    @Mapping(target = "updateBy", ignore = true)
    @Mapping(target = "updateTime", ignore = true)
    Customer dtoToEntity(CustomerDTO dto);

    CustomerVO entityToVo(Customer entity);

    List<CustomerVO> entityListToVoList(List<Customer> list);
}
