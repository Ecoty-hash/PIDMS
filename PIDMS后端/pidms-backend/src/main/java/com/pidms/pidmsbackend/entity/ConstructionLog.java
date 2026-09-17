package com.pidms.pidmsbackend.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 施工日志表 cm_construction_log
 * <p>按日记录现场施工情况；项目通过 project_id 关联 pm_project。</p>
 * 天气取值：sunny晴 / cloudy多云 / rainy雨 / snowy雪
 */
@Data
@TableName("cm_construction_log")
public class ConstructionLog {

    /** 主键ID */
    @TableId(type = IdType.AUTO)
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

    /** 创建人 */
    private String createBy;

    /** 创建时间 */
    private LocalDateTime createTime;

    /** 最后修改人 */
    private String updateBy;

    /** 最后修改时间 */
    private LocalDateTime updateTime;
}
