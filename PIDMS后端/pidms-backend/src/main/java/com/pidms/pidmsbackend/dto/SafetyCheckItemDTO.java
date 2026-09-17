package com.pidms.pidmsbackend.dto;

import lombok.Data;

/**
 * 安全检查项 新增 / 编辑 请求体
 */
@Data
public class SafetyCheckItemDTO {

    /** 主键ID（编辑时随请求体携带，与路径 /{id} 一致；新增不传） */
    private Long id;

    /** 所属分类：用电安全/高空作业/消防安全/机械安全 */
    private String category;

    /** 检查项名称（必填） */
    private String checkItemName;

    /** 状态；可选值：enabled启用 / disabled停用（新增默认 enabled，封存走专用接口） */
    private String status;
}
