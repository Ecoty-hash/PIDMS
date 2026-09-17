package com.pidms.pidmsbackend.convert;

import com.pidms.pidmsbackend.entity.ProjectDoc;
import com.pidms.pidmsbackend.vo.ProjectDocVO;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

import java.util.List;

@Mapper(componentModel = "spring")

public interface ProjectDocConvertMapper {

    // 单条 Entity → VO
    ProjectDocVO toVo(ProjectDoc entity);

    // 集合 List<Entity> → List<VO>
    List<ProjectDocVO> toVoList(List<ProjectDoc> list);
}
