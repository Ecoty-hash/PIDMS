package com.pidms.pidmsbackend.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 安全检查项 VO（列表 / 详情通用）
 */
@Data
public class SafetyCheckItemVO {

    /** 主键ID */
    private Long id;

    /** 所属分类：用电安全/高空作业/消防安全/机械安全 */
    private String category;

    /** 检查项名称 */
    private String checkItemName;

    /** 状态：enabled启用 / disabled停用 / sealed封存 */
    private String status;

    /** 创建人 */
    private String createBy;

    /** 创建时间 */
    private LocalDateTime createTime;

    /** 最后修改人 */
    private String updateBy;

    /** 最后修改时间 */
    private LocalDateTime updateTime;
}
