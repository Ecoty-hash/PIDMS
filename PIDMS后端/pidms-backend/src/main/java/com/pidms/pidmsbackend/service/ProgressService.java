package com.pidms.pidmsbackend.service;

import com.pidms.pidmsbackend.common.Result;
import com.pidms.pidmsbackend.dto.ProgressDTO;
import com.pidms.pidmsbackend.entity.PageInfo;
import com.pidms.pidmsbackend.entity.ProgressQueryParam;
import com.pidms.pidmsbackend.vo.ProgressBoardVO;
import com.pidms.pidmsbackend.vo.ProgressParentOptionVO;
import com.pidms.pidmsbackend.vo.ProgressVO;

import java.util.List;

/**
 * 进度管理 服务接口
 */
public interface ProgressService {

    /**
     * 分页查询进度管理列表
     */
    Result<PageInfo<ProgressVO>> pageQuery(ProgressQueryParam queryParam);

    /**
     * 项目进度详情页数据：项目基础信息 + 进度汇总 + 节点树 + 负责人候选
     */
    Result<ProgressBoardVO> board(Long projectId);

    /**
     * 上级节点候选：本项目下的节点（带层级），已排除 excludeId 自身及其子孙
     */
    Result<List<ProgressParentOptionVO>> parentOptions(Long projectId, Long excludeId);

    /**
     * 查询进度管理详情
     */
    Result<ProgressVO> getById(Long id);

    /**
     * 新增进度管理
     */
    Result<Long> add(ProgressDTO dto);

    /**
     * 编辑进度管理
     */
    Result<Long> update(Long id, ProgressDTO dto);

    /**
     * 删除进度管理
     */
    Result<Boolean> delete(Long id);
}
