package com.pidms.pidmsbackend.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@AllArgsConstructor
@NoArgsConstructor

public class ProjectQueryParam {
    //分页参数
    private Integer page = 1;
    private Integer pageSize = 10;
    /**
     * 关键字模糊搜索
     */
    private String keyword;

    /**
     * 项目名称
     */
    private String projectName;

    /**
     * 归档状态：未归档/已归档
     */
    private String archiveStatus;

    /**
     * 开工日期-开始
     */
    private LocalDate startDateBegin;

    /**
     * 开工日期-结束
     */
    private LocalDate startDateEnd;

    /**
     * 竣工日期-开始
     */
    private LocalDate completionDateBegin;

    /**
     * 竣工日期-结束
     */
    private LocalDate completionDateEnd;

    /**
     * 项目状态：规划中/在建/已完工
     */
    private String projectStatus;
}
