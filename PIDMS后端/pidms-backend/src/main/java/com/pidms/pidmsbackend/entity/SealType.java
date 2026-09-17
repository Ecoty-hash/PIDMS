package com.pidms.pidmsbackend.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 印章类型表 pm_seal_type
 * <p>维护印章类型字典（公章/合同章/财务章等），供用印申请选择，支持封存/解封。</p>
 * 状态取值：enabled启用 / disabled停用 / sealed封存
 */
@Data
@TableName("pm_seal_type")
public class SealType {

    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 印章类型名称
     */
    private String typeName;

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
