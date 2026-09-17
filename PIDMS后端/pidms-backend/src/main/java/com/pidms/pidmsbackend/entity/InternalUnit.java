package com.pidms.pidmsbackend.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 内部单位实体 bas_internal_unit
 */
@Data
@TableName("bas_internal_unit")
public class InternalUnit {

    /** 主键ID */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 单位编号 */
    private String unitCode;

    /** 单位名称 */
    private String unitName;

    /** 单位类型：branch分公司 / project项目部 / department部门 */
    private String unitType;

    /** 负责人 */
    private String manager;

    /** 联系人 */
    private String contactPerson;

    /** 联系电话 */
    private String contactPhone;

    /** 办公地址 */
    private String officeAddress;

    /** 状态：enabled启用 / disabled停用 */
    private String status;

    /** 备注 */
    private String remark;

    /** 图片（文件名/URL） */
    private String image;

    /** 附件（文件名，多个以顿号分隔） */
    private String attachments;
    /** 创建人 */
    private String createBy;

    /** 创建时间 */
    private LocalDateTime createTime;

    /** 最后修改人 */
    private String updateBy;

    /** 最后修改时间 */
    private LocalDateTime updateTime;
}
