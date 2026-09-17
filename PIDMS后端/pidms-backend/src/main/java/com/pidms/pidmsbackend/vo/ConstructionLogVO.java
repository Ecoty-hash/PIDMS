package com.pidms.pidmsbackend.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 施工日志 VO（列表 / 详情通用）
 * <p>projectName 为展示字段，由后端联查 pm_project 填充（表内不落库）。</p>
 */
@Data
public class ConstructionLogVO {

    /** 主键ID */
    private Long id;

    /** 项目ID → pm_project.id */
    private Long projectId;

    /** 项目名称（联查展示，非落库字段） */
    private String projectName;

    /** 对应计划名称 */
    private String planName;

    /** 日志日期 */
    private LocalDate logDate;

    /** 天气：sunny晴 / cloudy多云 / rainy雨 / snowy雪 */
    private String weather;

    /** 施工部位 */
    private String constructionLocation;

    /** 施工内容 */
    private String constructionContent;

    /** 工作量 */
    private BigDecimal workload;

    /** 出勤人数 */
    private Integer attendanceCount;

    /** 施工详情 */
    private String constructionDetail;

    /** 存在问题 */
    private String existingProblems;

    /** 图片（sys_attachment 路径数组，JSON 字符串） */
    private String images;

    /** 创建人 */
    private String createBy;

    /** 创建时间 */
    private LocalDateTime createTime;

    /** 最后修改人 */
    private String updateBy;

    /** 最后修改时间 */
    private LocalDateTime updateTime;
}
