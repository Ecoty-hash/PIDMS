package com.pidms.pidmsbackend.vo;

import lombok.Data;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
public class ProjectVO {

    private Long id;
    private String projectName;
    private String projectCode;
    private String ownerUnit;
    private String constructionUnit;
    private String constructionAddress;
    private String projectLeader;
    private String projectMembers;
    private String participationStatus;
    private String archiveStatus;
    private String projectStatus;
    private LocalDate startDate;
    private LocalDate completionDate;
    private String remark;

    // 详情页展示字段
    private String projectOverview;
    private String createBy;
    private LocalDateTime createTime;
    private String updateBy;
    private LocalDateTime updateTime;

    // 不要再手写get/set，@Data自动生成
}
