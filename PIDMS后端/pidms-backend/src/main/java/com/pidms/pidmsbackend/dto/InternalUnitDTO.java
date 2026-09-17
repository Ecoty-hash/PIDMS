package com.pidms.pidmsbackend.dto;

import lombok.Data;

/**
 * 内部单位 新增 / 编辑 请求体
 */
@Data
public class InternalUnitDTO {

    /** 主键ID（编辑时随请求体携带，与路径 /{id} 一致；新增不传） */
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
}
