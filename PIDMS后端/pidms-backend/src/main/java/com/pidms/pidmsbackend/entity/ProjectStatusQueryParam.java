package com.pidms.pidmsbackend.entity;

import lombok.Data;

/**
 * 项目状态分页查询参数（直接接收 GET 查询参数，与接口文档一一对应）
 */
@Data
public class ProjectStatusQueryParam {

    /**
     * 页码，默认 1
     */
    private Integer page;

    /**
     * 每页条数，默认 20
     */
    private Integer pageSize;

    /**
     * 关键字模糊搜索（状态名称 / 状态编码）
     */
    private String keyword;

    /**
     * 状态；可选值：enabled启用 / disabled停用 / sealed封存
     */
    private String status;
}
