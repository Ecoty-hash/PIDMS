package com.pidms.pidmsbackend.vo;

import lombok.Data;

import java.time.LocalDate;

/**
 * 工作台 - 最近施工日志
 */
@Data
public class WorkbenchLogVO {

    private Long id;

    private Long projectId;

    private String projectName;

    /** 计划名称 */
    private String planName;

    private LocalDate logDate;

    private String weather;

    private String constructionLocation;

    private String constructionContent;

    /** 记录人 */
    private String createBy;
}
