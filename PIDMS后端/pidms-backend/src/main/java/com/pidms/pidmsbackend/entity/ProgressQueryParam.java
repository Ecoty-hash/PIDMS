package com.pidms.pidmsbackend.entity;

import lombok.Data;

/**
 * 进度管理 分页查询参数
 * <p>projectName 按项目名称过滤（后端需先匹配 pm_project 再回查 cm_progress）。</p>
 */
@Data
public class ProgressQueryParam {

    /** 页码，默认1 */
    private Long page;

    /** 每页条数，默认20 */
    private Long pageSize;

    /** 关键字模糊搜索（进度名称/进度编号/项目名称） */
    private String keyword;

    /** 项目名称（高级搜索） */
    private String projectName;

    /** 进度名称（高级搜索） */
    private String progressName;

    /** 进度状态（高级搜索） */
    private String progressStatus;
}
