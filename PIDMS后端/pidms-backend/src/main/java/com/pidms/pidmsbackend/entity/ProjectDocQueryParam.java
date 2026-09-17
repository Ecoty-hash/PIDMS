package com.pidms.pidmsbackend.entity;

import lombok.Data;

/**
 * 项目文档列表查询请求参数，直接接收前端get查询参数，字段与接口文档一一对应
 */
@Data
public class ProjectDocQueryParam {

    /**
     * 页码，默认1
     */
    private Integer page;

    /**
     * 每页条数，默认20
     */
    private Integer pageSize;

    /**
     * 关键字模糊搜索
     */
    private String keyword;

    /**
     * 文档标题
     */
    private String docTitle;

    /**
     * 所属项目id
     */
    private String projectId;

    /**
     * 文档分类：施工图纸/技术资料/验收资料
     */
    private String docCategory;

    /**
     * 上传日期
     */
    private String uploadDate;
}
