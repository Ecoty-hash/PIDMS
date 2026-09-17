package com.pidms.pidmsbackend.service;

import com.pidms.pidmsbackend.common.Result;
import com.pidms.pidmsbackend.dto.ProjectBudgetDTO;
import com.pidms.pidmsbackend.entity.PageInfo;
import com.pidms.pidmsbackend.entity.ProjectBudgetQueryParam;
import com.pidms.pidmsbackend.vo.ProjectBudgetVO;

public interface ProjectBudgetService {
    Result<PageInfo<ProjectBudgetVO>> pageQuery(ProjectBudgetQueryParam projectBudgetQueryParam);

    //新增功能
    Result<Long> addBudget(ProjectBudgetDTO projectBudgetDTO);

    //编辑功能
    Result<Long> updateBudget(Long id, ProjectBudgetDTO projectBudgetDTO);

    //删除功能
    Result<Boolean> deleteBudget(Long id);

    //查询功能
    Result<ProjectBudgetVO> getById(Long id);
}
