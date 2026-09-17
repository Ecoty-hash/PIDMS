package com.pidms.pidmsbackend.vo;

import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 用印申请 VO（列表 / 详情通用）
 * <p>除业务字段外，附带 projectName / sealTypeName 供列表与表单直接展示。</p>
 */
@Data
public class SealApplicationVO {

    private Long id;

    /**
     * 单号
     */
    private String billNo;

    /**
     * 标题
     */
    private String title;

    /**
     * 项目ID
     */
    private Long projectId;

    /**
     * 项目名称（关联带出）
     */
    private String projectName;

    /**
     * 申请人
     */
    private String applicant;

    /**
     * 申请日期
     */
    private LocalDate applyDate;

    /**
     * 用印部门
     */
    private String sealDepartment;

    /**
     * 印章类型ID
     */
    private Long sealTypeId;

    /**
     * 印章类型名称（关联带出）
     */
    private String sealTypeName;

    /**
     * 用印文件名称
     */
    private String sealFileName;

    /**
     * 文件份数
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
     * 审批状态：pending待审批 / approved已审批 / rejected已拒绝
     */
    private String approvalStatus;

    /**
     * 审批人
     */
    private String approver;

    /**
     * 备注
     */
    private String remark;

    /**
     * 创建人
     */
    private String createBy;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 最后修改人
     */
    private String updateBy;

    /**
     * 最后修改时间
     */
    private LocalDateTime updateTime;
}
