package com.pidms.pidmsbackend.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 施工日志 新增 / 编辑 请求体
 * <p>项目以 projectId 提交；projectName 由后端联查返回展示。</p>
 */
@Data
public class ConstructionLogDTO {

    /** 主键ID（编辑时随请求体携带，与路径 /{id} 一致；新增不传） */
    private Long id;

    /** 项目ID → pm_project.id（必填） */
    private Long projectId;

    /** 对应计划名称 */
    private String planName;

    /** 日志日期（必填） */
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
}
