package com.pidms.pidmsbackend.dto;

import lombok.Data;

import java.time.LocalDate;

/**
 * 用印申请 新增 / 编辑 请求体
 * <p>项目与印章类型按「名称」提交（与预算模块一致）：前端下拉绑定 projectName / sealTypeName，
 * 后端据此解析 project_id / seal_type_id。审批状态由审批动作接口维护，编辑不会覆盖。</p>
 */
@Data
public class SealApplicationDTO {

    /**
     * 主键ID（编辑时随请求体携带，与路径 /{id} 一致；新增不传）
     */
    private Long id;

    /**
     * 项目名称（必填，后端据此解析 project_id）
     */
    private String projectName;

    /**
     * 标题（必填）
     */
    private String title;

    /**
     * 单号（选填；留空由后端自动生成，格式：YY-年份-4位流水，如 YY-2026-0006）
     */
    private String billNo;

    /**
     * 申请日期（必填）
     */
    private LocalDate applyDate;

    /**
     * 申请人（必填）
     */
    private String applicant;

    /**
     * 用印部门（必填）
     */
    private String sealDepartment;

    /**
     * 印章类型名称（必填，后端据此解析 seal_type_id）
     */
    private String sealTypeName;

    /**
     * 用印文件名称（必填）
     */
    private String sealFileName;

    /**
     * 文件份数（必填；未传按 1）
     */
    private Integer fileCopies;

    /**
     * 用印方式：原件盖章 / 复印件盖章
     */
    private String sealMethod;

    /**
     * 用印说明
     */
    private String sealDescription;

    /**
     * 备注
     */
    private String remark;
}
