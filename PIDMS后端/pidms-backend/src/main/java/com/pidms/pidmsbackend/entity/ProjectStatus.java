package com.pidms.pidmsbackend.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 项目状态定义表 pm_project_status
 * <p>维护项目状态字典（如 规划中/在建/已完工），支持封存/解封。</p>
 * 状态取值：enabled启用 / disabled停用 / sealed封存
 */
@Data
@TableName("pm_project_status")
public class ProjectStatus {

    @TableId(type = IdType.AUTO)
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
