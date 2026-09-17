package com.pidms.pidmsbackend.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 用印申请表 pm_seal_application
 * <p>项目用印申请流程管理：登记用印文件、份数、方式、印章类型等信息，支持审批流转。</p>
 * 审批状态：pending待审批 / approved已审批 / rejected已拒绝
 */
@Data
@TableName("pm_seal_application")
public class SealApplication {

    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 单号（唯一；新增未填时后端自动生成）
     */
    private String billNo;

    /**
     * 标题
     */
    private String title;

    /**
     * 项目ID → pm_project.id
     */
    private Long projectId;

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
     * 印章类型ID → pm_seal_type.id
     */
    private Long sealTypeId;

    /**
     * 用印文件名称
     */
    private String sealFileName;

    /**
     * 文件份数（默认 1）
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
