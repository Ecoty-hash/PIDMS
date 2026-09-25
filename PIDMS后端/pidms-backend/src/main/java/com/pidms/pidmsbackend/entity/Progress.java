package com.pidms.pidmsbackend.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 进度管理实体 cm_progress
 * <p>与数据库设计一致：通过 project_id 外键关联 pm_project；不落冗余的 project_name / plan_duration。</p>
 */
@Data
@TableName("cm_progress")
public class Progress {

    /** 主键ID */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 项目ID → pm_project.id */
    private Long projectId;

    /** 父节点ID → cm_progress.id；NULL 为顶层节点（支持多层子节点） */
    private Long parentId;

    /** 进度名称 */
    private String progressName;

    /** 进度编号 */
    private String progressCode;

    /** 计划开始日期 */
    private LocalDate planStartDate;

    /** 计划结束日期 */
    private LocalDate planEndDate;

    /** 实际开始日期 */
    private LocalDate actualStartDate;

    /** 实际结束日期 */
    private LocalDate actualEndDate;

    /** 完成百分比(0-100)；有子节点时不参与计算，页面显示的是子节点加权汇总值 */
    private BigDecimal completionPercent;

    /** 占总进度百分比(0-100)，只有叶子节点需要填；未填时按同组均分 */
    private BigDecimal weight;

    /** 进度状态：in-progress进行中 / completed已完成 / delayed延期 */
    private String progressStatus;

    /** 节点负责人（真实姓名），候选来源 pm_project.project_members；可为空 */
    private String responsiblePerson;

    /** 备注（页面上的「节点描述」） */
    private String remark;

    /** 创建人 */
    private String createBy;

    /** 创建时间 */
    private LocalDateTime createTime;

    /** 最后修改人 */
    private String updateBy;

    /** 最后修改时间 */
    private LocalDateTime updateTime;
}
