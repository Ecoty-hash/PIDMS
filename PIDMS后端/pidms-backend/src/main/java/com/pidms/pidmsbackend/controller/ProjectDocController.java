package com.pidms.pidmsbackend.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.pidms.pidmsbackend.common.Result;
import com.pidms.pidmsbackend.dto.ProjectDocQueryDTO;
import com.pidms.pidmsbackend.entity.PageInfo;
import com.pidms.pidmsbackend.entity.ProjectDocQueryParam;
import com.pidms.pidmsbackend.service.ProjectDocService;
import com.pidms.pidmsbackend.vo.ProjectDocVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/project-docs")
@RequiredArgsConstructor
 @Tag(name = "项目文档接口")
public class ProjectDocController {
    private final ProjectDocService projectDocService;
    @GetMapping
    @Operation(summary = "项目文档分页查询")
    public Result<PageInfo<ProjectDocVO>> pageList(ProjectDocQueryParam projectDocQueryParam) {
        PageInfo<ProjectDocVO> pageInfo = projectDocService.queryPage(projectDocQueryParam);
        return Result.success(pageInfo);
    }
    //
}
