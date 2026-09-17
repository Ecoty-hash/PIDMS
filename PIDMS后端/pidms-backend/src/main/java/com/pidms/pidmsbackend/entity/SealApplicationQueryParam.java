package com.pidms.pidmsbackend.entity;

import lombok.Data;

import java.time.LocalDate;

/**
 * 用印申请分页查询参数（直接接收 GET 查询参数，与接口文档一一对应）
 * <p>项目与印章类型按「名称」筛选：后端先在 pm_project / pm_seal_type 中按名称匹配，
 * 再回落到项目ID / 印章类型ID 精确过滤（与列表 VO 带出的名称列一致）。</p>
 */
@Data
public class SealApplicationQueryParam {

    /**
     * 页码，默认 1
     */
    private Integer page;

    /**
     * 每页条数，默认 20
     */
    private Integer pageSize;

    /**
     * 关键字模糊搜索（单号 / 标题 / 用印文件名称 / 备注）
     */
    private String keyword;

    /**
     * 项目名称（模糊）
     */
    private String projectName;

    /**
     * 标题（模糊）
     */
    private String title;

    /**
     * 审批状态；可选值：pending待审批 / approved已审批 / rejected已拒绝
     */
    private String approvalStatus;

    /**
     * 用印部门（模糊）
     */
    private String sealDepartment;

    /**
     * 用印方式：原件盖章 / 复印件盖章
     */
    private String sealMethod;

    /**
     * 印章类型名称（模糊）
     */
    private String sealTypeName;

    /**
     * 申请人（模糊）
     */
    private String applicant;

    /**
     * 申请日期（精确到天）
     */
    private LocalDate applyDate;
}
