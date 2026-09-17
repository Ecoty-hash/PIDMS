package com.pidms.pidmsbackend.service;


import com.pidms.pidmsbackend.dto.ProjectSaveDTO;
import com.pidms.pidmsbackend.entity.PageInfo;
import com.pidms.pidmsbackend.entity.ProjectQueryParam;
import com.pidms.pidmsbackend.vo.ProjectVO;


public interface ProjectService {
    PageInfo<ProjectVO> pageQuery(ProjectQueryParam projectQueryParam); //分页查询项目

    ProjectVO getProjectDetailById(Long id);

    void addProject(ProjectSaveDTO dto, String operateUser);

    void updateProject(ProjectSaveDTO dto, String operateUser);

    void deleteById(Long id,String operateUser);

    void updateProjectStatus(Long id, String operateUser);

    void batchNumber(Long[] ids, String operateUsername);
}
