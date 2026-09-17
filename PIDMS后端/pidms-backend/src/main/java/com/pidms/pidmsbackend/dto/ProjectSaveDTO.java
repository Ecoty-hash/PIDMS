package com.pidms.pidmsbackend.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ProjectSaveDTO {
    /** 修改的时候传id，新增不传 */
    private Long id;

    @NotBlank(message = "项目名称不能为空")
    private String projectName;

    @NotBlank(message = "项目编号不能为空")
    private String projectCode;

    private String startDate;
    private String completionDate;
    private String projectStatus;
    private String constructionUnit;
    private String constructionAddress;
    private String archiveStatus;
    private String projectLeader;
    private String projectMembers;
    private String participationStatus;
    private String ownerUnit;
    private String projectOverview;
    private String remark;
}
