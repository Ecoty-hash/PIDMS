package com.pidms.pidmsbackend.vo;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 基础资料 VO（列表 / 详情通用）
 */
@Data
public class BasicInfoVO {

    /** 主键ID */
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
