package com.pidms.pidmsbackend.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 质量检查表 cm_quality_inspection
 * <p>一次质量检查记录，项目通过 project_id 外键关联 pm_project；明细项存 cm_quality_inspection_item。</p>
 * 检查结果取值：pending待检查 / qualified合格 / unqualified不合格
 */
@Data
@TableName("cm_quality_inspection")
public class QualityInspection {

    /** 主键ID */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 检查单号（唯一，可空） */
    private String inspectionNo;

    /** 项目ID → pm_project.id */
    private Long projectId;

    /** 检查类型：quality质量检查（默认 quality） */
    private String inspectionType;

    /** 检查部门 */
    private String inspectionDept;

    /** 检查日期 */
    private LocalDate inspectionDate;

    /** 检查部位 */
    private String inspectionLocation;

    /** 检查人 */
    private String inspector;

    /** 检查结果：pending待检查 / qualified合格 / unqualified不合格 */
    private String inspectionResult;

    /** 检查详情 */
    private String inspectionDetail;

    /** 创建人 */
    private String createBy;

    /** 创建时间 */
    private LocalDateTime createTime;

    /** 最后修改人 */
    private String updateBy;

    /** 最后修改时间 */
    private LocalDateTime updateTime;
}
