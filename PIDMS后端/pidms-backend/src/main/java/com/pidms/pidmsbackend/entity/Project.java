package com.pidms.pidmsbackend.entity;

import java.time.LocalDateTime;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("pm_project")
public class Project {

    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 项目名称
     */
    private String projectName;

    /**
     * 项目编号，文档字段projectCode
     */
    private String projectCode;

    /**
     * 甲方单位
     */
    private String ownerUnit;

    /**
     * 施工单位
     */
    private String constructionUnit;

    /**
     * 施工地址
     */
    private String constructionAddress;

    /**
     * 开工日期
     */
    private LocalDateTime startDate;

    /**
     * 竣工日期
     */
    private LocalDateTime completionDate;

    /**
     * 项目状态
     */
    private String projectStatus;

    /**
     * 归档状态：未归档/已归档
     */
    private String archiveStatus;

    /**
     * 项目负责人
     */
    private String projectLeader;

    /**
     * 项目成员
     */
    private String projectMembers;

    /**
     * 参与状态
     */
    private String participationStatus;

    /**
     * 项目概况
     */
    private String projectOverview;

    private String remark;

    private String createBy;

    private LocalDateTime createTime;

    private String updateBy;

    private LocalDateTime updateTime;
}
