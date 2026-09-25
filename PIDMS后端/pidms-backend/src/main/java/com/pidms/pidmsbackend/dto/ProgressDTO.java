package com.pidms.pidmsbackend.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 进度管理 新增 / 编辑 请求体
 * <p>项目以 projectId 提交（数据库仅存外键 project_id）；projectName 由后端联查返回展示。</p>
 */
@Data
public class ProgressDTO {

    /** 主键ID（编辑时随请求体携带，与路径 /{id} 一致；新增不传） */
    private Long id;

    /** 项目ID → pm_project.id（必填） */
    private Long projectId;

    /** 父节点ID → cm_progress.id；不传或传 null 表示顶层节点（子节点填父节点 id） */
    private Long parentId;

    /**
     * 置为顶层节点：编辑时把 parent_id 清空。
     * <p>编辑接口沿用「null = 本次不修改」的语义，所以「把子节点提到顶层」必须靠这个显式标记，
     * 否则 parentId 为 null 会被当成不修改（和 actual_end_date 同一个坑）。</p>
     */
    private Boolean topLevel;

    /** 进度名称（必填） */
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

    /**
     * 完成百分比(0-100)。节点有子节点时该值不参与项目进度计算（页面显示的是子节点加权汇总值），
     * 但仍原样保存：子节点被删完之后这个手填值会重新生效。
     */
    private BigDecimal completionPercent;

    /**
     * 占总进度百分比(0-100)，只有叶子节点需要填（父节点的份额由子节点汇总得出，填了也不参与计算）。
     * 不填表示与同组未填的兄弟节点平分「100 − 已填之和」；整组都没填即均分。
     */
    private BigDecimal weight;

    /** 进度状态：in-progress进行中 / completed已完成 / delayed延期（新增默认 in-progress） */
    private String progressStatus;

    /** 节点负责人（真实姓名），可为空 */
    private String responsiblePerson;

    /** 备注（页面上的「节点描述」） */
    private String remark;
}
