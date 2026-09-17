package com.pidms.pidmsbackend.convert;

import com.pidms.pidmsbackend.dto.ConstructionLogDTO;
import com.pidms.pidmsbackend.entity.ConstructionLog;
import com.pidms.pidmsbackend.vo.ConstructionLogVO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

/**
 * 施工日志 转换器
 * <p>DTO→Entity 忽略审计字段（由 Service 补全）；Entity→VO 后 projectName 由 Service 联查补全。</p>
 */
@Mapper(componentModel = "spring")
public interface ConstructionLogConvert {

    @Mapping(target = "createTime", ignore = true)
    @Mapping(target = "updateTime", ignore = true)
    @Mapping(target = "createBy", ignore = true)
    @Mapping(target = "updateBy", ignore = true)
    ConstructionLog dtoToEntity(ConstructionLogDTO dto);

    @Mapping(target = "projectName", ignore = true)
    ConstructionLogVO entityToVo(ConstructionLog entity);

    List<ConstructionLogVO> entityListToVoList(List<ConstructionLog> list);
}
