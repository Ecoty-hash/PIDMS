package com.pidms.pidmsbackend.convert;

import com.pidms.pidmsbackend.dto.MakeupApplicationDTO;
import com.pidms.pidmsbackend.entity.MakeupApplication;
import com.pidms.pidmsbackend.vo.MakeupApplicationVO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

/**
 * 补卡申请 转换器
 * <p>DTO→Entity 忽略审计字段（由 Service 补全）。</p>
 */
@Mapper(componentModel = "spring")
public interface MakeupApplicationConvert {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createBy", ignore = true)
    @Mapping(target = "createTime", ignore = true)
    @Mapping(target = "updateBy", ignore = true)
    @Mapping(target = "updateTime", ignore = true)
    MakeupApplication dtoToEntity(MakeupApplicationDTO dto);

    MakeupApplicationVO entityToVo(MakeupApplication entity);

    List<MakeupApplicationVO> entityListToVoList(List<MakeupApplication> list);
}
