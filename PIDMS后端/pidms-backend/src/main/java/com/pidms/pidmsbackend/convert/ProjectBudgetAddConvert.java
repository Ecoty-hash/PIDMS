package com.pidms.pidmsbackend.convert;

import com.pidms.pidmsbackend.dto.ProjectBudgetDTO;
import com.pidms.pidmsbackend.entity.ProjectBudget;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

// 一期平面预算：明细子表(pm_project_budget_item)业务不读写，
// 下列子表转换相关 import 注释保留，待二期「多行明细录入」启用后再放开。
// import com.pidms.pidmsbackend.dto.BudgetItemDTO;
// import com.pidms.pidmsbackend.entity.BudgetItem;
// import java.util.List;

@Mapper
public interface ProjectBudgetAddConvert {
    ProjectBudgetAddConvert INSTANCE = Mappers.getMapper(ProjectBudgetAddConvert.class);
    // 主表 DTO → Entity
    ProjectBudget addDtoToEntity(ProjectBudgetDTO dto);

    // 子表 DTO → Entity 集合转换（一期预留不启用，二期启用多行明细录入时再放开）
    // List<BudgetItem> itemDtoListToEntityList(List<BudgetItemDTO> dtoList);
}
