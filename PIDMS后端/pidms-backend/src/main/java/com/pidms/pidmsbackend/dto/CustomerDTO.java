package com.pidms.pidmsbackend.dto;

import lombok.Data;

/**
 * 客户 新增 / 编辑 请求体
 */
@Data
public class CustomerDTO {

    /** 主键ID（编辑时随请求体携带，与路径 /{id} 一致；新增不传） */
    private Long id;

    /** 客户编号 */
    private String customerCode;

    /** 客户名称 */
    private String customerName;

    /** 客户类型：government政府单位 / enterprise企业客户 / state-owned国企客户 */
    private String customerType;

    /** 联系人 */
    private String contactPerson;

    /** 联系电话 */
    private String contactPhone;

    /** 地址 */
    private String address;

    /** 办公地址 */
    private String officeAddress;

    /** 发票抬头 */
    private String invoiceTitle;

    /** 税号 */
    private String taxNo;

    /** 电话 */
    private String phone;

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
}
