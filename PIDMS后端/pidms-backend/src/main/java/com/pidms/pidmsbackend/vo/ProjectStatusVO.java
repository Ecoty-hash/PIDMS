package com.pidms.pidmsbackend.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 项目状态 VO（列表 / 详情通用）
 */
@Data
public class ProjectStatusVO {

    private Long id;

    /**
     * 项目状态名称
     */
    private String statusName;

    /**
     * 状态编码
     */
    private String statusCode;

    /**
     * 状态：enabled启用 / disabled停用 / sealed封存
     */
    private String status;

    /**
     * 创建人
     */
    private String createBy;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 最后修改人
     */
    private String updateBy;

    /**
     * 最后修改时间
     */
    private LocalDateTime updateTime;
}
