package com.pidms.pidmsbackend.entity;

import lombok.Data;

/**
 * 预警规则 分页查询参数
 * <p>作为 GET 请求参数绑定；page / pageSize 用于分页。</p>
 */
@Data
public class WarningRuleQueryParam {

    /** 页码，从 1 开始 */
    private Long page;

    /** 每页条数 */
    private Long pageSize;

    /** 项目名称（模糊，按名称反查项目 id） */
    private String projectName;

    /** 规则名称（模糊） */
    private String ruleName;

    /** 预警类型：进度停滞预警 / 质量问题预警 / 安全预警 */
    private String warningType;

    /** 状态：enabled启用 / disabled禁用 */
    private String status;
}
