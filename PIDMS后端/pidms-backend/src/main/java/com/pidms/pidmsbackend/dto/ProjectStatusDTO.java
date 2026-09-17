package com.pidms.pidmsbackend.dto;

import lombok.Data;

/**
 * 项目状态 新增 / 编辑 请求体
 * <p>状态未传时后端默认 enabled；编辑时仅覆盖请求体中存在的字段。</p>
 */
@Data
public class ProjectStatusDTO {

    /**
     * 主键ID（编辑时随请求体携带，与路径 /{id} 一致；新增不传）
     */
    private Long id;

    /**
     * 项目状态名称（必填）
     */
    private String statusName;

    /**
     * 状态编码（可选）
     */
    private String statusCode;

    /**
     * 状态；可选值：enabled启用 / disabled停用（新增默认 enabled，封存/解封走专用接口）
     */
    private String status;
}
