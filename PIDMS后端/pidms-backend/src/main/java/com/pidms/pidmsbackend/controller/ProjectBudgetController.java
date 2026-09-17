package com.pidms.pidmsbackend.controller;

import com.pidms.pidmsbackend.common.Result;
import com.pidms.pidmsbackend.dto.ProjectBudgetDTO;
import com.pidms.pidmsbackend.entity.PageInfo;
import com.pidms.pidmsbackend.entity.ProjectBudgetQueryParam;
import com.pidms.pidmsbackend.service.ProjectBudgetService;
import com.pidms.pidmsbackend.vo.ProjectBudgetVO;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/project-budgets")
public class ProjectBudgetController {
    @Resource
    private ProjectBudgetService projectBudgetService;
    @GetMapping
    //分页查询
    public Result<PageInfo<ProjectBudgetVO>> pageList(ProjectBudgetQueryParam projectBudgetQueryParam) {
        log.info("分页查询项目预算，参数：{}", projectBudgetQueryParam);
        return projectBudgetService.pageQuery(projectBudgetQueryParam);
    }
    @GetMapping("/{id}")
    public Result<ProjectBudgetVO> getById(@PathVariable Long id) {
        log.info("根据id查询项目预算，参数：{}", id);
        return projectBudgetService.getById(id);
    }
    //新增功能
    @PostMapping
    public Result<Long> addBudget(@RequestBody ProjectBudgetDTO projectBudgetDTO) {
        log.info("新增项目预算，参数：{}", projectBudgetDTO);
        return projectBudgetService.addBudget(projectBudgetDTO);
    }
    //编辑功能
    @PutMapping("/{id}")
    public Result<Long> updateBudget(@PathVariable Long id, @RequestBody ProjectBudgetDTO projectBudgetDTO) {
        log.info("编辑项目预算，id：{}，参数：{}", id, projectBudgetDTO);
        return projectBudgetService.updateBudget(id, projectBudgetDTO);
    }
    //删除功能
    @DeleteMapping("/{id}")
    public Result<Boolean> deleteBudget(@PathVariable Long id) {
        log.info("删除项目预算，参数：{}", id);
        return projectBudgetService.deleteBudget(id);
    }
}
