package com.pidms.pidmsbackend.convert;

import com.pidms.pidmsbackend.entity.ProjectBudget;
import com.pidms.pidmsbackend.vo.ProjectBudgetVO;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

@Mapper
public interface ProjectBudgetConvert {
    ProjectBudgetConvert INSTANCE = Mappers.getMapper(ProjectBudgetConvert.class);

    ProjectBudgetVO entityToVo(ProjectBudget entity);

}
