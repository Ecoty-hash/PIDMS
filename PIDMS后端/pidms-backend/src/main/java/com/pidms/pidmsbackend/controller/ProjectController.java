package com.pidms.pidmsbackend.controller;

import com.pidms.pidmsbackend.common.Result;
import com.pidms.pidmsbackend.common.UserContext;
import com.pidms.pidmsbackend.dto.ProjectSaveDTO;
import com.pidms.pidmsbackend.entity.PageInfo;
import com.pidms.pidmsbackend.entity.ProjectQueryParam;
import com.pidms.pidmsbackend.service.ProjectService;
import com.pidms.pidmsbackend.vo.ProjectVO;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/projects")
public class ProjectController {

    @Resource
    private ProjectService projectService;
    @Autowired
    private UserContext userContext;

    /**
     * 分页列表查询
     * @param projectQueryParam 查询条件(分页+筛选字段)，不需要转换Entity，service内部构建QueryWrapper
     * @return 分页数据 ProjectVO
     */
    @GetMapping
    public Result<PageInfo<ProjectVO>> pageList(ProjectQueryParam projectQueryParam) {
        log.info("分页查询项目，参数：{}", projectQueryParam);
        PageInfo<ProjectVO> pageInfo = projectService.pageQuery(projectQueryParam);
        return Result.success(pageInfo);
    }

    /**
     * 根据id查询项目详情
     * @param id 项目主键
     * @return 项目详情 ProjectVO
     */
    @GetMapping("/{id}")
    public Result<ProjectVO> getDetail(@PathVariable Long id) {
        log.info("查询项目详情，id：{}", id);
        ProjectVO vo = projectService.getProjectDetailById(id);
        return Result.success(vo);
    }

    /**
     * 新增项目
     * @param dto 前端提交表单数据DTO
     */
    @PostMapping
    public Result<Void> add(@Valid @RequestBody ProjectSaveDTO dto) {
        log.info("新增项目，参数：{}", dto);
        String operateUsername = UserContext.getUser().getUsername();
        projectService.addProject(dto, operateUsername);
        return Result.success(null);
    }

    /**
     * 修改项目
     * @param dto 前端提交表单数据DTO
     */
    @PutMapping("/{id}")
    public Result update(@Valid @PathVariable Long id, @RequestBody ProjectSaveDTO dto ) {
        log.info("修改项目，参数：{}", dto);
        //绑定需要修改的项目id
        dto.setId(id);
        String operateUsername = UserContext.getUser().getUsername();
        projectService.updateProject(dto, operateUsername);
        return Result.success(id);
    }

    /**
     * 删除项目
     * @param id 项目主键
     */
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id){
        String operateUsername = userContext.getUser().getUsername();
        log.info("删除项目，id：{}",id);
        projectService.deleteById(id,operateUsername);
        return Result.success(null);
    }
    /**
     * 归档项目（与接口文档 3.1.8 一致：POST /api/projects/{id}/archive）
     * @param id 项目主键
     */
    @PostMapping("/{id}/archive")
    public Result<Void> archive(@PathVariable Long id){
        String operateUsername = userContext.getUser().getUsername();
        log.info("归档项目，id：{}",id);
        projectService.updateProjectStatus(id,operateUsername);
        return Result.success(null);
    }
    /**
     * 批量生成编号
     */
    @PostMapping("/batch-number")
    public Result<Void> batchNumber(@RequestBody Long[] ids){
        String operateUsername = userContext.getUser().getUsername();
        log.info("批量生成编号");
        projectService.batchNumber(ids,operateUsername);
        return Result.success(null);
    }

}
