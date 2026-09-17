package com.pidms.pidmsbackend.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 基础资料实体 bas_basic_info
 */
@Data
@TableName("bas_basic_info")
public class BasicInfo {

    /** 主键ID */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 资料编号 */
    private String infoCode;

    /** 资料名称 */
    private String infoName;

    /** 分类：material材料分类 / equipment设备分类 / worktype工种分类 */
    private String infoCategory;

    /** 规格型号 */
    private String specification;

    /** 单位 */
    private String unit;

    /** 参考单价 */
    private BigDecimal referencePrice;

    /** 状态：active在用 / archived已归档 */
    private String status;

    /** 备注 */
    private String remark;
    /** 创建人 */
    private String createBy;

    /** 创建时间 */
    private LocalDateTime createTime;

    /** 最后修改人 */
    private String updateBy;

    /** 最后修改时间 */
    private LocalDateTime updateTime;
}
