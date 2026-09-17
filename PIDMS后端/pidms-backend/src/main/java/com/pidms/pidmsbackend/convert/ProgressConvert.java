package com.pidms.pidmsbackend.convert;

import com.pidms.pidmsbackend.dto.ProgressDTO;
import com.pidms.pidmsbackend.entity.Progress;
import com.pidms.pidmsbackend.vo.ProgressVO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

/**
 * 进度管理 转换器
 * <p>DTO→Entity 忽略审计字段（由 Service 补全）；Entity→VO 后 projectName 由 Service 联查补全。</p>
 */
@Mapper(componentModel = "spring")
public interface ProgressConvert {

    @Mapping(target = "createTime", ignore = true)
    @Mapping(target = "updateTime", ignore = true)
    @Mapping(target = "createBy", ignore = true)
    @Mapping(target = "updateBy", ignore = true)
    Progress dtoToEntity(ProgressDTO dto);

    @Mapping(target = "projectName", ignore = true)
    ProgressVO entityToVo(Progress entity);

    List<ProgressVO> entityListToVoList(List<Progress> list);
}
