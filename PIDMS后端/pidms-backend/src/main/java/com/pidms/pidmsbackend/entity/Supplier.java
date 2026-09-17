package com.pidms.pidmsbackend.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 供应商实体 bas_supplier
 */
@Data
@TableName("bas_supplier")
public class Supplier {

    /** 主键ID */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 供应商编号 */
    private String supplierCode;

    /** 供应商名称 */
    private String supplierName;

    /** 供应商类型：material材料供应商 / equipment设备供应商 / labor劳务分包 */
    private String supplierType;

    /** 联系人 */
    private String contactPerson;

    /** 联系电话 */
    private String contactPhone;

    /** 联系地址 */
    private String contactAddress;

    /** 办公地址 */
    private String officeAddress;

    /** 报价人 */
    private String quoter;

    /** 报价人手机 */
    private String quoterPhone;

    /** 户名 */
    private String accountName;

    /** 开户行 */
    private String bankName;

    /** 银行卡号 */
    private String bankAccount;

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
