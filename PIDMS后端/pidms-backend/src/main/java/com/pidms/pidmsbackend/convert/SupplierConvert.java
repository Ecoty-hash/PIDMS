package com.pidms.pidmsbackend.convert;

import com.pidms.pidmsbackend.dto.SupplierDTO;
import com.pidms.pidmsbackend.entity.Supplier;
import com.pidms.pidmsbackend.vo.SupplierVO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

/**
 * 供应商 转换器
 * <p>DTO→Entity 忽略审计字段（由 Service 补全）。</p>
 */
@Mapper(componentModel = "spring")
public interface SupplierConvert {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createBy", ignore = true)
    @Mapping(target = "createTime", ignore = true)
    @Mapping(target = "updateBy", ignore = true)
    @Mapping(target = "updateTime", ignore = true)
    Supplier dtoToEntity(SupplierDTO dto);

    SupplierVO entityToVo(Supplier entity);

    List<SupplierVO> entityListToVoList(List<Supplier> list);
}
