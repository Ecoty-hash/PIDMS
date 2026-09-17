package com.pidms.pidmsbackend.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 人员管理 VO（列表 / 详情通用）
 */
@Data
public class PersonnelVO {

    /** 主键ID */
    private Long id;

    /** 姓名 */
    private String name;

    /** 性别：male男 / female女 */
    private String gender;

    /** 工号 */
    private String employeeNo;

    /** 电话 */
    private String phone;

    /** 角色ID → sys_role.id */
    private Long roleId;

    /** 角色名称（联查 sys_user_role + sys_role 回填，非落库字段） */
    private String roleName;

    /** 所属机构ID → sys_org.id */
    private Long orgId;

    /** 机构名称（联查 sys_org 回填，非落库字段） */
    private String orgName;

    /** 状态：enabled在职 / disabled离职 */
    private String status;

    /** 图片（文件名 / URL） */
    private String image;

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
