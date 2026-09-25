package com.pidms.pidmsbackend.vo;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 项目状态分布（可视化管理 - 状态分布环形图）
 */
@Data
public class ProjectStatusStatVO {

    /** 状态编码，取自 pm_project_status.status_code */
    private String statusCode;

    /** 状态名称（中文） */
    private String statusName;

    /** 该状态下的项目数 */
    private Long count;

    /** 占比，百分比，保留 1 位小数 */
    private BigDecimal percent;
}
