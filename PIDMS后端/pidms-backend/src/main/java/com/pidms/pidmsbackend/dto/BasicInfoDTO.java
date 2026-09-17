package com.pidms.pidmsbackend.dto;

import lombok.Data;
import java.math.BigDecimal;

/**
 * 基础资料 新增 / 编辑 请求体
 */
@Data
public class BasicInfoDTO {

    /** 主键ID（编辑时随请求体携带，与路径 /{id} 一致；新增不传） */
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
}
