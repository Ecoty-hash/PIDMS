package com.pidms.pidmsbackend.dto;

import lombok.Data;

/**
 * 人员管理 新增 / 编辑 请求体
 * <p>说明：本模块只维护人员档案与角色分配，不创建登录账号
 * （sys_user.username / password 在本模块不写入，留待「账号管理」另做）。</p>
 */
@Data
public class PersonnelDTO {

    /** 主键ID（编辑时随请求体携带，与路径 /{id} 一致；新增不传） */
    private Long id;

    /** 姓名 */
    private String name;

    /** 性别：male男 / female女（兼容历史值 男 / 女，落库前归一） */
    private String gender;

    /** 工号 */
    private String employeeNo;

    /** 电话 */
    private String phone;

    /** 角色ID → sys_role.id */
    private Long roleId;

    /** 所属机构ID → sys_org.id */
    private Long orgId;

    /** 状态：enabled在职 / disabled离职 */
    private String status;

    /** 图片（文件名 / URL） */
    private String image;

    /** 备注 */
    private String remark;
}
