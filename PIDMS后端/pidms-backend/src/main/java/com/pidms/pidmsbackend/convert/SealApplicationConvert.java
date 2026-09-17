package com.pidms.pidmsbackend.convert;

import com.pidms.pidmsbackend.dto.SealApplicationDTO;
import com.pidms.pidmsbackend.entity.SealApplication;
import com.pidms.pidmsbackend.vo.SealApplicationVO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface SealApplicationConvert {

    // 请求体 → 实体：projectName / sealTypeName 由 Service 解析为 projectId / sealTypeId 后回填；
    // 审批状态、审批人、审计字段由 Service 维护，此处不映射。
    @Mapping(target = "projectId", ignore = true)
    @Mapping(target = "sealTypeId", ignore = true)
    @Mapping(target = "approvalStatus", ignore = true)
    @Mapping(target = "approver", ignore = true)
    @Mapping(target = "createBy", ignore = true)
    @Mapping(target = "createTime", ignore = true)
    @Mapping(target = "updateBy", ignore = true)
    @Mapping(target = "updateTime", ignore = true)
    SealApplication dtoToEntity(SealApplicationDTO dto);

    SealApplicationVO entityToVo(SealApplication entity);

    List<SealApplicationVO> entityListToVoList(List<SealApplication> list);
}
