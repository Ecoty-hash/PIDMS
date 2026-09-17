package com.pidms.pidmsbackend.dto;

import lombok.Data;

/**
 * 工种类型 新增 / 编辑 请求体
 */
@Data
public class WorkTypeDTO {

    /** 主键ID（编辑时随请求体携带，与路径 /{id} 一致；新增不传） */
    private Long id;

    /** 工种类型名称 */
    private String typeName;

    /** 状态：enabled启用 / disabled停用 / sealed封存 */
    private String status;
}
