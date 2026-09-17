package com.pidms.pidmsbackend.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.pidms.pidmsbackend.convert.ProjectDocConvertMapper;
import com.pidms.pidmsbackend.entity.PageInfo;
import com.pidms.pidmsbackend.entity.ProjectDoc;
import com.pidms.pidmsbackend.entity.ProjectDocQueryParam;
import com.pidms.pidmsbackend.mapper.ProjectDocMapper;
import com.pidms.pidmsbackend.service.ProjectDocService;
import com.pidms.pidmsbackend.vo.ProjectDocVO;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

@Slf4j
@Service
public class ProjectDocServiceImpl extends ServiceImpl<ProjectDocMapper, ProjectDoc> implements ProjectDocService {

    @Resource
    private ProjectDocConvertMapper projectDocConvertMapper;

    @Override
    public PageInfo<ProjectDocVO> queryPage(ProjectDocQueryParam queryParam) {
        // 设置分页默认值
        Integer pageNum = queryParam.getPage() == null ? 1 : queryParam.getPage();
        Integer pageSize = queryParam.getPageSize() == null ? 20 : queryParam.getPageSize();

        log.info("pageNum: {}, pageSize: {}", pageNum, pageSize);

        QueryWrapper<ProjectDoc> queryWrapper = new QueryWrapper<>();

        // 关键字模糊搜索 doc_title / doc_name
        if (StrUtil.isNotBlank(queryParam.getKeyword())) {
            queryWrapper.and(w -> w.like("doc_title", queryParam.getKeyword()).or().like("doc_name", queryParam.getKeyword()));
        }
        if (StrUtil.isNotBlank(queryParam.getDocTitle())) {
            queryWrapper.like("doc_title", queryParam.getDocTitle());
        }
        if (StrUtil.isNotBlank(queryParam.getProjectId())) {
            queryWrapper.eq("project_id", queryParam.getProjectId());
        }
        if (StrUtil.isNotBlank(queryParam.getDocCategory())) {
            queryWrapper.eq("doc_category", queryParam.getDocCategory());
        }
        if (StrUtil.isNotBlank(queryParam.getUploadDate())) {
            queryWrapper.eq("upload_time", queryParam.getUploadDate());
        }

        // 分页查询，继承ServiceImpl直接使用 baseMapper，不需要额外注入mapper
        Page<ProjectDoc> page = baseMapper.selectPage(new Page<>(pageNum, pageSize), queryWrapper);

        PageInfo<ProjectDocVO> pageInfo = new PageInfo<>();
        pageInfo.setPage(pageNum);
        pageInfo.setPageSize(pageSize);
        pageInfo.setTotal(page.getTotal());

        // 判空保护，避免null传入转换方法
        if(!CollectionUtils.isEmpty(page.getRecords())){
            pageInfo.setList(projectDocConvertMapper.toVoList(page.getRecords()));
        }

        return pageInfo;
    }
//
//    @Override
//    public ProjectDocVO getDocDetail(Long id) {
//        ProjectDoc entity = baseMapper.selectById(id);
//        return projectDocConvertMapper.toVo(entity);
//    }
}
