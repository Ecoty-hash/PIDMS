package com.pidms.pidmsbackend.convert;

import com.pidms.pidmsbackend.dto.LeaveApplicationDTO;
import com.pidms.pidmsbackend.entity.LeaveApplication;
import com.pidms.pidmsbackend.vo.LeaveApplicationVO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

/**
 * 请假申请 转换器
 * <p>DTO→Entity 忽略审计字段（由 Service 补全）。</p>
 */
@Mapper(componentModel = "spring")
public interface LeaveApplicationConvert {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createBy", ignore = true)
    @Mapping(target = "createTime", ignore = true)
    @Mapping(target = "updateBy", ignore = true)
    @Mapping(target = "updateTime", ignore = true)
    LeaveApplication dtoToEntity(LeaveApplicationDTO dto);

    LeaveApplicationVO entityToVo(LeaveApplication entity);

    List<LeaveApplicationVO> entityListToVoList(List<LeaveApplication> list);
}
