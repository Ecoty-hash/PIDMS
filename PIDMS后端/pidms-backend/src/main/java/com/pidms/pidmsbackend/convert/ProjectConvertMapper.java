package com.pidms.pidmsbackend.convert;

import com.pidms.pidmsbackend.dto.ProjectSaveDTO;
import com.pidms.pidmsbackend.entity.Project;
import com.pidms.pidmsbackend.vo.ProjectVO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface ProjectConvertMapper {
    ProjectConvertMapper INSTANCE = Mappers.getMapper(ProjectConvertMapper.class);

    ProjectVO entityToVo(Project project);

    List<ProjectVO> entityListToVoList(List<Project> list);

    @Mapping(target = "createBy", ignore = true)
    @Mapping(target = "createTime", ignore = true)
    @Mapping(target = "updateBy", ignore = true)
    @Mapping(target = "updateTime", ignore = true)
    Project dtoToEntity(ProjectSaveDTO dto);

    /**
     * String 日期 → LocalDateTime，兼容 date 输入框的 "yyyy-MM-dd" 和 ISO-8601 完整时间。
     * MapStruct 会自动用该方法转换 DTO 中的 startDate / completionDate。
     */
    default LocalDateTime stringToLocalDateTime(String date) {
        if (date == null || date.isBlank()) {
            return null;
        }
        String s = date.trim();
        try {
            return LocalDateTime.parse(s);
        } catch (Exception e) {
            return LocalDate.parse(s).atStartOfDay();
        }
    }
}
