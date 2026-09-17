package com.pidms.pidmsbackend.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 安全检查项表 cm_safety_check_item
 * <p>维护安全检查项字典（用电安全/高空作业/消防安全/机械安全），供安全检查单引用，支持封存。</p>
 * 状态取值：enabled启用 / disabled停用 / sealed封存
 */
@Data
@TableName("cm_safety_check_item")
public class SafetyCheckItem {

    /** 主键ID */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 所属分类：用电安全/高空作业/消防安全/机械安全 */
    private String category;

    /** 检查项名称（必填） */
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
