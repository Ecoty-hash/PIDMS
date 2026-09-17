package com.pidms.pidmsbackend.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 工种类型实体 bas_work_type
 */
@Data
@TableName("bas_work_type")
public class WorkType {

    /** 主键ID */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 工种类型名称 */
    private String typeName;

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
