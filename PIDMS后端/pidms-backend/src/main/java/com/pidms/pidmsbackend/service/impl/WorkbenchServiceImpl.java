package com.pidms.pidmsbackend.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.pidms.pidmsbackend.common.LoginUser;
import com.pidms.pidmsbackend.common.Result;
import com.pidms.pidmsbackend.common.UserContext;
import com.pidms.pidmsbackend.entity.BusinessTrip;
import com.pidms.pidmsbackend.entity.ConstructionLog;
import com.pidms.pidmsbackend.entity.LeaveApplication;
import com.pidms.pidmsbackend.entity.MakeupApplication;
import com.pidms.pidmsbackend.entity.Progress;
import com.pidms.pidmsbackend.entity.Project;
import com.pidms.pidmsbackend.entity.SealApplication;
import com.pidms.pidmsbackend.mapper.BusinessTripMapper;
import com.pidms.pidmsbackend.mapper.ConstructionLogMapper;
import com.pidms.pidmsbackend.mapper.LeaveApplicationMapper;
import com.pidms.pidmsbackend.mapper.MakeupApplicationMapper;
import com.pidms.pidmsbackend.mapper.ProgressMapper;
import com.pidms.pidmsbackend.mapper.ProjectMapper;
import com.pidms.pidmsbackend.mapper.SealApplicationMapper;
import com.pidms.pidmsbackend.service.WorkbenchService;
import com.pidms.pidmsbackend.service.support.ProjectProgressCalculator;
import com.pidms.pidmsbackend.vo.ProjectProgressVO;
import com.pidms.pidmsbackend.vo.WorkbenchApplicationVO;
import com.pidms.pidmsbackend.vo.WorkbenchLogVO;
import com.pidms.pidmsbackend.vo.WorkbenchRiskVO;
import com.pidms.pidmsbackend.vo.WorkbenchTodoVO;
import com.pidms.pidmsbackend.vo.WorkbenchVO;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 工作台 服务实现（个人视角）
 *
 * 身份比对说明：sys_user 的 name（真实姓名）才是业务表里的「人」
 * 项目负责人、项目成员、申请人、审批人存的都是真实姓名；而 create_by 存的是登录用户名。
 * 所以这里同时用 realName 和 username 去比对，避免换个人登录就什么都看不到。
 *
 * 口径：
 * - 待我审批 = approval_status='pending' 且（审批人是我 或 审批人未指派）
 * - 我的项目 = 我是项目负责人 或 项目成员里有我 或 我创建的
 * - 逾期预警 = 我的项目下 计划结束日已过且未完成的形象进度节点
 * - 最近动态 = 我的项目下最近 6 条施工日志
 */
@Slf4j
@Service
public class WorkbenchServiceImpl implements WorkbenchService {

    private static final String APPROVAL_PENDING = "pending";

    private static final int MAX_RISKS = 6;
    private static final int MAX_LOGS = 6;
    private static final int MAX_APPLICATIONS = 5;

    @Resource
    private ProjectMapper projectMapper;
    @Resource
    private ProgressMapper progressMapper;
    @Resource
    private ConstructionLogMapper constructionLogMapper;
    @Resource
    private SealApplicationMapper sealApplicationMapper;
    @Resource
    private LeaveApplicationMapper leaveApplicationMapper;
    @Resource
    private MakeupApplicationMapper makeupApplicationMapper;
    @Resource
    private BusinessTripMapper businessTripMapper;
    @Resource
    private ProjectProgressCalculator progressCalculator;

    @Override
    public Result<WorkbenchVO> overview() {
        LoginUser loginUser = UserContext.getUser();
        if (loginUser == null) {
            return Result.error("未登录或登录已过期");
        }
        String username = loginUser.getUsername();
        String realName = loginUser.getRealName();
        String me = (realName == null || realName.isBlank()) ? username : realName;
        LocalDate today = LocalDate.now();

        WorkbenchVO vo = new WorkbenchVO();
        vo.setUsername(username);
        vo.setRealName(realName);
        vo.setRole(loginUser.getRole());
        vo.setGeneratedAt(LocalDateTime.now());

        // ---------------- 待我审批 ----------------
        List<WorkbenchTodoVO> todos = new ArrayList<>();
        todos.add(toTodo("seal-application", "用印申请", sealApplicationMapper.selectList(
                new LambdaQueryWrapper<SealApplication>()
                        .eq(SealApplication::getApprovalStatus, APPROVAL_PENDING)
                        .and(w -> w.isNull(SealApplication::getApprover)//判断审批人是否为空
                                .or().eq(SealApplication::getApprover, "")//判断审批人是否为空格
                                .or().eq(SealApplication::getApprover, me))),//判断审批人是否为我
                SealApplication::getApprover));
        todos.add(toTodo("leave-application", "请假申请", leaveApplicationMapper.selectList(
                new LambdaQueryWrapper<LeaveApplication>()
                        .eq(LeaveApplication::getApprovalStatus, APPROVAL_PENDING)
                        .and(w -> w.isNull(LeaveApplication::getApprover)
                                .or().eq(LeaveApplication::getApprover, "")
                                .or().eq(LeaveApplication::getApprover, me))),
                LeaveApplication::getApprover));
        todos.add(toTodo("makeup-application", "补卡申请", makeupApplicationMapper.selectList(
                new LambdaQueryWrapper<MakeupApplication>()
                        .eq(MakeupApplication::getApprovalStatus, APPROVAL_PENDING)
                        .and(w -> w.isNull(MakeupApplication::getApprover)
                                .or().eq(MakeupApplication::getApprover, "")
                                .or().eq(MakeupApplication::getApprover, me))),
                MakeupApplication::getApprover));
        todos.add(toTodo("business-trip", "出差申请", businessTripMapper.selectList(
                new LambdaQueryWrapper<BusinessTrip>()
                        .eq(BusinessTrip::getApprovalStatus, APPROVAL_PENDING)
                        .and(w -> w.isNull(BusinessTrip::getApprover)
                                .or().eq(BusinessTrip::getApprover, "")
                                .or().eq(BusinessTrip::getApprover, me))),
                BusinessTrip::getApprover));
        // 只保留有待办的模块，避免工作台上一排 0
        List<WorkbenchTodoVO> pendingTodos = todos.stream()
                .filter(t -> t.getCount() != null && t.getCount() > 0)
                .collect(Collectors.toList());
        vo.setTodos(pendingTodos);
        vo.setTodoTotal(pendingTodos.stream().mapToLong(WorkbenchTodoVO::getCount).sum());

        // ---------------- 我的项目 ----------------
        List<Project> allProjects = projectMapper.selectList(null);
        List<Project> myProjects = allProjects.stream()
                .filter(p -> isMine(p, me, username))
                .collect(Collectors.toList());
        List<Long> myProjectIds = myProjects.stream().map(Project::getId).collect(Collectors.toList());
        Map<Long, List<Progress>> nodesByProject = progressCalculator.groupByProject(progressMapper.selectList(null));
        Map<String, String> statusNames = progressCalculator.statusNameMap();

        List<ProjectProgressVO> myRows = myProjects.stream()
                .map(p -> progressCalculator.build(p,
                        nodesByProject.getOrDefault(p.getId(), Collections.emptyList()), statusNames, today))
                .sorted(Comparator.comparing(ProjectProgressVO::getProgressDelta)
                        .thenComparing(ProjectProgressVO::getProjectId,
                                Comparator.nullsLast(Comparator.naturalOrder())))
                .collect(Collectors.toList());
        vo.setMyProjects(myRows);
        vo.setMyProjectCount(myRows.size());
        vo.setMyAvgProgress(progressCalculator.avgValues(
                myRows.stream().map(ProjectProgressVO::getActualProgress).collect(Collectors.toList())));

        // ---------------- 逾期节点预警 ----------------
        // 只报叶子节点：父节点的逾期是子树汇总出来的，报出来会和子节点重复，也不可执行
        List<WorkbenchRiskVO> risks = new ArrayList<>();
        for (Project project : myProjects) {
            Map<Long, ProjectProgressCalculator.NodeMetrics> metrics = progressCalculator.metricsOf(
                    nodesByProject.getOrDefault(project.getId(), Collections.emptyList()), today);
            for (ProjectProgressCalculator.NodeMetrics metric : progressCalculator.leavesOf(metrics)) {
                Long overdueDays = metric.getOverdueDays();
                if (overdueDays == null) {
                    continue;
                }
                Progress node = metric.getNode();
                WorkbenchRiskVO risk = new WorkbenchRiskVO();
                risk.setProjectId(project.getId());
                risk.setProjectName(project.getProjectName());
                risk.setNodeId(node.getId());
                risk.setNodeName(node.getProgressName());
                risk.setProjectLeader(project.getProjectLeader());
                risk.setPlanEndDate(metric.getEffectivePlanEnd());
                risk.setOverdueDays(overdueDays);
                risk.setSeverity(progressCalculator.severity(overdueDays));
                risk.setCompletionPercent(metric.getCompletion());
                risks.add(risk);
            }
        }
        risks.sort(Comparator.comparing(WorkbenchRiskVO::getOverdueDays).reversed());
        vo.setOverdueNodeCount(risks.size());
        vo.setRisks(risks.size() > MAX_RISKS ? new ArrayList<>(risks.subList(0, MAX_RISKS)) : risks);

        // ---------------- 最近施工日志 ----------------
        if (myProjectIds.isEmpty()) {
            vo.setRecentLogs(Collections.emptyList());
        } else {
            List<ConstructionLog> logs = constructionLogMapper.selectList(new LambdaQueryWrapper<ConstructionLog>()
                    .in(ConstructionLog::getProjectId, myProjectIds)
                    .orderByDesc(ConstructionLog::getLogDate)
                    .orderByDesc(ConstructionLog::getCreateTime)
                    .last("LIMIT " + MAX_LOGS));
            Map<Long, String> projectNames = allProjects.stream()
                    .collect(Collectors.toMap(Project::getId, Project::getProjectName, (a, b) -> a));
            vo.setRecentLogs(logs.stream()
                    .map(log -> toLog(log, projectNames.get(log.getProjectId())))
                    .collect(Collectors.toList()));
        }

        // ---------------- 我发起的申请 ----------------
        List<WorkbenchApplicationVO> applications = new ArrayList<>();
        sealApplicationMapper.selectList(new LambdaQueryWrapper<SealApplication>()
                        .eq(SealApplication::getApplicant, me)
                        .orderByDesc(SealApplication::getApplyDate)
                        .last("LIMIT " + MAX_APPLICATIONS))
                .forEach(item -> {
                    WorkbenchApplicationVO app = baseApplication("seal-application", "用印申请");
                    app.setBillNo(item.getBillNo());
                    app.setTitle(item.getTitle());
                    app.setApprovalStatus(item.getApprovalStatus());
                    app.setApprover(item.getApprover());
                    // 用印只有申请日期，补成当天 00:00 参与统一排序
                    app.setApplyTime(item.getApplyDate() == null ? null : item.getApplyDate().atStartOfDay());
                    applications.add(app);
                });
        leaveApplicationMapper.selectList(new LambdaQueryWrapper<LeaveApplication>()
                        .eq(LeaveApplication::getApplicant, me)
                        .orderByDesc(LeaveApplication::getApplyTime)
                        .last("LIMIT " + MAX_APPLICATIONS))
                .forEach(item -> {
                    WorkbenchApplicationVO app = baseApplication("leave-application", "请假申请");
                    app.setBillNo(item.getApplyNo());
                    app.setApprovalStatus(item.getApprovalStatus());
                    app.setApprover(item.getApprover());
                    app.setApplyTime(item.getApplyTime());
                    applications.add(app);
                });
        makeupApplicationMapper.selectList(new LambdaQueryWrapper<MakeupApplication>()
                        .eq(MakeupApplication::getApplicant, me)
                        .orderByDesc(MakeupApplication::getApplyTime)
                        .last("LIMIT " + MAX_APPLICATIONS))
                .forEach(item -> {
                    WorkbenchApplicationVO app = baseApplication("makeup-application", "补卡申请");
                    app.setBillNo(item.getApplyNo());
                    app.setApprovalStatus(item.getApprovalStatus());
                    app.setApprover(item.getApprover());
                    app.setApplyTime(item.getApplyTime());
                    applications.add(app);
                });
        businessTripMapper.selectList(new LambdaQueryWrapper<BusinessTrip>()
                        .eq(BusinessTrip::getApplicant, me)
                        .orderByDesc(BusinessTrip::getApplyTime)
                        .last("LIMIT " + MAX_APPLICATIONS))
                .forEach(item -> {
                    WorkbenchApplicationVO app = baseApplication("business-trip", "出差申请");
                    app.setBillNo(item.getApplyNo());
                    app.setApprovalStatus(item.getApprovalStatus());
                    app.setApprover(item.getApprover());
                    app.setApplyTime(item.getApplyTime());
                    applications.add(app);
                });
        applications.sort(Comparator.comparing(WorkbenchApplicationVO::getApplyTime,
                Comparator.nullsLast(Comparator.reverseOrder())));
        vo.setMyApplications(applications.size() > MAX_APPLICATIONS
                ? new ArrayList<>(applications.subList(0, MAX_APPLICATIONS)) : applications);

        vo.setDescription("待我审批含「审批人未指派」的单据；我的项目 = 我是项目负责人、项目成员或创建人；"
                + "逾期预警 = 我的项目下计划结束日已过且未完成的形象进度节点，基准日期 " + today + "。");
        return Result.success(vo);
    }

    // ==================================================================
    //  内部方法
    // ==================================================================

    /** 组装一项待办，并统计其中未指派审批人的条数 */
    private <T> WorkbenchTodoVO toTodo(String moduleKey, String moduleName, List<T>       pending,
                                       Function<T, String> approverGetter) {
        //
        long unassigned = pending.stream().filter(item -> {
            String approver = approverGetter.apply(item);
            return approver == null || approver.isBlank();
        }).count();
        WorkbenchTodoVO todo = new WorkbenchTodoVO();
        todo.setModuleKey(moduleKey);
        todo.setModuleName(moduleName);
        todo.setCount((long) pending.size());
        todo.setUnassignedCount(unassigned);
        return todo;
    }

    /** 项目是否属于我：负责人 / 项目成员 / 创建人 */
    private boolean isMine(Project project, String me, String username) {
        if (me.equals(project.getProjectLeader())) {
            return true;
        }
        if (me.equals(project.getCreateBy()) || (username != null && username.equals(project.getCreateBy()))) {
            return true;
        }
        String members = project.getProjectMembers();
        if (members != null && !members.isBlank()) {
            for (String member : members.split(",")) {
                if (me.equals(member.trim())) {
                    return true;
                }
            }
        }
        return false;
    }

    private WorkbenchApplicationVO baseApplication(String moduleKey, String moduleName) {
        WorkbenchApplicationVO vo = new WorkbenchApplicationVO();
        vo.setModuleKey(moduleKey);
        vo.setModuleName(moduleName);
        return vo;
    }

    private WorkbenchLogVO toLog(ConstructionLog log, String projectName) {
        WorkbenchLogVO vo = new WorkbenchLogVO();
        vo.setId(log.getId());
        vo.setProjectId(log.getProjectId());
        vo.setProjectName(projectName);
        vo.setPlanName(log.getPlanName());
        vo.setLogDate(log.getLogDate());
        vo.setWeather(log.getWeather());
        vo.setConstructionLocation(log.getConstructionLocation());
        vo.setConstructionContent(log.getConstructionContent());
        vo.setCreateBy(log.getCreateBy());
        return vo;
    }
}
