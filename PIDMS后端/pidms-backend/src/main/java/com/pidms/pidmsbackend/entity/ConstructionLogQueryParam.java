package com.pidms.pidmsbackend.entity;

import lombok.Data;

import java.time.LocalDate;

/**
 * 施工日志 分页查询参数
 * <p>作为 GET 请求参数绑定；page / pageSize 用于分页。</p>
 */
@Data
public class ConstructionLogQueryParam {

    /** 页码，从 1 开始 */
    private Long page;

    /** 每页条数 */
    private Long pageSize;

    /** 项目名称（模糊，按名称反查项目 id） */
    private String projectName;

    /** 天气：sunny晴 / cloudy多云 / rainy雨 / snowy雪 */
    private String weather;

    /** 日志日期 */
    private LocalDate logDate;

    /** 施工部位（模糊） */
    private String constructionLocation;
}
