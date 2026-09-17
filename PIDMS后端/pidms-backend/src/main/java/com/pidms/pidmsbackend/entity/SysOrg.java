package com.pidms.pidmsbackend.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 机构（机构管理）—— 对应 sys_org 表。
 * <p>树形结构：parent_id 指向上级机构，顶级机构 parent_id 为 NULL。</p>
 */
@Data
@TableName("sys_org")
public class SysOrg {

    /** 主键ID */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 上级机构ID，顶级为 NULL */
    private Long parentId;

    /** 机构编码 */
    private String orgCode;

    /** 机构名称 */
    private String orgName;

    /** 机构顺序 */
    private Integer sortOrder;

    /** 描述 */
    private String description;

    /** 状态：enabled启用 / disabled停用 */
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
