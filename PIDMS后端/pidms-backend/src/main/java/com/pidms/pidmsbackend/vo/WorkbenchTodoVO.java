package com.pidms.pidmsbackend.vo;

import lombok.Data;

/**
 * 工作台 - 待我审批的一项
 */
@Data
public class WorkbenchTodoVO {

    /** 模块 key，与前端 modules.js 的 key 一致，用于跳转 /{moduleKey} */
    private String moduleKey;

    /** 模块名称 */
    private String moduleName;

    /** 待审批条数（含未指派审批人的单据） */
    private Long count;

    /** 其中「未指派审批人」的条数，前端可提示“含 N 条未指派” */
    private Long unassignedCount;
}
