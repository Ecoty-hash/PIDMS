package com.pidms.pidmsbackend.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 工作台总览（/api/workbench/overview）
 *
 * 全部口径都以「当前登录用户」为中心：待我审批、我负责的项目、与我相关的逾期节点、
 * 我参与项目的最近施工日志、我发起的申请。
 */
@Data
public class WorkbenchVO {

    private String username;

    /** 真实姓名（sys_user.name），项目负责人/申请人/审批人比对的都是它 */
    private String realName;

    private String role;

    /** 数据生成时间，前端用于显示“刚刚更新” */
    private LocalDateTime generatedAt;

    /** 统计口径说明，前端放在页面角落，避免数字被误读 */
    private String description;

    // ---------------- 待我审批 ----------------

    /** 待我审批总条数 */
    private Long todoTotal;

    /** 分模块明细（只包含条数 > 0 的模块） */
    private List<WorkbenchTodoVO> todos;

    // ---------------- 我负责的项目 ----------------

    /** 我负责的项目数 */
    private Integer myProjectCount;

    /** 我负责项目的平均实际进度（%） */
    private BigDecimal myAvgProgress;

    /** 我负责的项目（含进度、偏差、逾期节点数） */
    private List<ProjectProgressVO> myProjects;

    // ---------------- 逾期节点预警 ----------------

    /** 与我相关的逾期未完成节点总数 */
    private Integer overdueNodeCount;

    /** 逾期节点明细，按逾期天数倒序，最多 6 条 */
    private List<WorkbenchRiskVO> risks;

    // ---------------- 最近动态 ----------------

    /** 我负责项目的最近施工日志，最多 6 条 */
    private List<WorkbenchLogVO> recentLogs;

    /** 我发起的申请（用印/请假/补卡/出差），按申请时间倒序，最多 5 条 */
    private List<WorkbenchApplicationVO> myApplications;
}
