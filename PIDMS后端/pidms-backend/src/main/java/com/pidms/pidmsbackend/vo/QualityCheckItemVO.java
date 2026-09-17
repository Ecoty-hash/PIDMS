package com.pidms.pidmsbackend.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 质量检查项 VO（列表 / 详情通用）
 */
@Data
public class QualityCheckItemVO {

    /** 主键ID */
    private Long id;

    /** 所属分类：结构工程/装饰工程/安装工程/防水工程 */
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
