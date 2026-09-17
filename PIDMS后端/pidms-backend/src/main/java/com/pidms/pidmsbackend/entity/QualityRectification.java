package com.pidms.pidmsbackend.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 质量整改单表 cm_quality_rectification
 * <p>质量检查不合格后发起整改，可关联质量检查单；项目通过 project_id 关联 pm_project。</p>
 * 整改状态取值：pending待整改 / processing整改中 / completed已整改
 */
@Data
@TableName("cm_quality_rectification")
public class QualityRectification {

    /** 主键ID */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 整改单号（唯一，可空） */
    private String rectificationNo;

    /** 项目ID → pm_project.id */
    private Long projectId;

    /** 关联检查单ID → cm_quality_inspection.id（可空） */
    private Long relatedInspectionId;

    /** 整改部位 */
    private String rectificationLocation;

    /** 要求完成日期 */
    private LocalDate requiredCompleteDate;

    /** 整改状态：pending待整改 / processing整改中 / completed已整改 */
    private String rectificationStatus;

    /** 责任人 */
    private String responsiblePerson;

    /** 整改内容 */
    private String rectificationContent;

    /** 创建人 */
    private String createBy;

    /** 创建时间 */
    private LocalDateTime createTime;

    /** 最后修改人 */
    private String updateBy;

    /** 最后修改时间 */
    private LocalDateTime updateTime;
}
