package com.pidms.pidmsbackend.controller;

import com.pidms.pidmsbackend.common.Result;
import com.pidms.pidmsbackend.dto.ProjectStatusDTO;
import com.pidms.pidmsbackend.entity.PageInfo;
import com.pidms.pidmsbackend.entity.ProjectStatusQueryParam;
import com.pidms.pidmsbackend.service.ProjectStatusService;
import com.pidms.pidmsbackend.vo.ProjectStatusVO;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/project-statuses")
public class ProjectStatusController {

    @Resource
    private ProjectStatusService projectStatusService;

    /**
     * 分页查询项目状态列表
     */
    @GetMapping
    public Result<PageInfo<ProjectStatusVO>> pageList(ProjectStatusQueryParam queryParam) {
        log.info("分页查询项目状态，参数：{}", queryParam);
        return projectStatusService.pageQuery(queryParam);
    }

    /**
     * 查询项目状态详情
     */
    @GetMapping("/{id}")
    public Result<ProjectStatusVO> getById(@PathVariable Long id) {
        log.info("根据id查询项目状态，id：{}", id);
        return projectStatusService.getById(id);
    }

    /**
     * 新增项目状态
     */
    @PostMapping
    public Result<Long> add(@RequestBody ProjectStatusDTO dto) {
        log.info("新增项目状态，参数：{}", dto);
        return projectStatusService.add(dto);
    }

    /**
     * 编辑项目状态
     */
    @PutMapping("/{id}")
    public Result<Long> update(@PathVariable Long id, @RequestBody ProjectStatusDTO dto) {
        log.info("编辑项目状态，id：{}，参数：{}", id, dto);
        return projectStatusService.update(id, dto);
    }

    /**
     * 删除项目状态
     */
    @DeleteMapping("/{id}")
    public Result<Boolean> delete(@PathVariable Long id) {
        log.info("删除项目状态，id：{}", id);
        return projectStatusService.delete(id);
    }

    /**
     * 封存项目状态
     */
    @PostMapping("/{id}/seal")
    public Result<Boolean> seal(@PathVariable Long id) {
        log.info("封存项目状态，id：{}", id);
        return projectStatusService.seal(id);
    }

    /**
     * 解封项目状态
     */
    @PostMapping("/{id}/unseal")
    public Result<Boolean> unseal(@PathVariable Long id) {
        log.info("解封项目状态，id：{}", id);
        return projectStatusService.unseal(id);
    }
}
