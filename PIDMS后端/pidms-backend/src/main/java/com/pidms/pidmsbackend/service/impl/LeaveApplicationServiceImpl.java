package com.pidms.pidmsbackend.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.pidms.pidmsbackend.common.Result;
import com.pidms.pidmsbackend.common.UserContext;
import com.pidms.pidmsbackend.convert.LeaveApplicationConvert;
import com.pidms.pidmsbackend.dto.LeaveApplicationDTO;
import com.pidms.pidmsbackend.entity.LeaveApplication;
import com.pidms.pidmsbackend.entity.LeaveApplicationQueryParam;
import com.pidms.pidmsbackend.entity.PageInfo;
import com.pidms.pidmsbackend.mapper.LeaveApplicationMapper;
import com.pidms.pidmsbackend.service.LeaveApplicationService;
import com.pidms.pidmsbackend.vo.LeaveApplicationVO;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.List;

/**
 * 请假申请 服务实现
 * <p>审批状态：pending未审批 / approved已通过 / rejected已驳回，流转走 /approve、/reject 动作接口。</p>
 */
@Slf4j
@Service
public class LeaveApplicationServiceImpl implements LeaveApplicationService {

    /** leaveType：annual */
    private static final String LEAVE_TYPE_ANNUAL = "annual";
    /** leaveType：personal */
    private static final String LEAVE_TYPE_PERSONAL = "personal";
    /** leaveType：sick */
    private static final String LEAVE_TYPE_SICK = "sick";
    /** leaveType：marriage */
    private static final String LEAVE_TYPE_MARRIAGE = "marriage";
    /** leaveType：maternity */
    private static final String LEAVE_TYPE_MATERNITY = "maternity";
    /** leaveType：compensatory */
    private static final String LEAVE_TYPE_COMPENSATORY = "compensatory";

    /** approvalStatus：pending */
    private static final String APPROVAL_STATUS_PENDING = "pending";
    /** approvalStatus：approved */
    private static final String APPROVAL_STATUS_APPROVED = "approved";
    /** approvalStatus：rejected */
    private static final String APPROVAL_STATUS_REJECTED = "rejected";

    @Resource
    private LeaveApplicationMapper leaveApplicationMapper;

    @Resource
    private LeaveApplicationConvert leaveApplicationConvert;

    @Override
    public Result<PageInfo<LeaveApplicationVO>> pageQuery(LeaveApplicationQueryParam queryParam) {
        long pageNum = queryParam.getPage() == null ? 1 : queryParam.getPage();
        long pageSize = queryParam.getPageSize() == null ? 20 : queryParam.getPageSize();
        log.info("分页查询请假申请：pageNum = {}, pageSize = {}, 参数 = {}", pageNum, pageSize, queryParam);

        QueryWrapper<LeaveApplication> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq(StringUtils.hasText(queryParam.getApplyNo()), "apply_no", queryParam.getApplyNo());
        queryWrapper.eq(StringUtils.hasText(queryParam.getApplicant()), "applicant", queryParam.getApplicant());
        queryWrapper.eq(StringUtils.hasText(queryParam.getLeaveType()), "leave_type", queryParam.getLeaveType());
        queryWrapper.eq(StringUtils.hasText(queryParam.getApprovalStatus()), "approval_status", queryParam.getApprovalStatus());
        queryWrapper.ge(queryParam.getStartDate() != null, "start_date", queryParam.getStartDate());
        queryWrapper.le(queryParam.getEndDate() != null, "end_date", queryParam.getEndDate());

        if (StringUtils.hasText(queryParam.getKeyword())) {
            String keyword = queryParam.getKeyword().trim();
            queryWrapper.and(w -> w
                        .like("apply_no", keyword)
                        .or().like("applicant", keyword)
                        .or().like("leave_reason", keyword));
        }
        queryWrapper.orderByAsc("id");

        Page<LeaveApplication> page = new Page<>(pageNum, pageSize);
        Page<LeaveApplication> resultPage = leaveApplicationMapper.selectPage(page, queryWrapper);
        List<LeaveApplication> records = resultPage.getRecords();
        log.info("分页查询请假申请结果条数：{}", records.size());

        PageInfo<LeaveApplicationVO> pageInfo = new PageInfo<>();
        pageInfo.setPage((int) resultPage.getCurrent());
        pageInfo.setPageSize((int) resultPage.getSize());
        pageInfo.setTotal(resultPage.getTotal());
        pageInfo.setPages((int) ((resultPage.getTotal() + pageSize - 1) / pageSize));
        pageInfo.setList(leaveApplicationConvert.entityListToVoList(records));
        return Result.success(pageInfo);
    }

    @Override
    public Result<LeaveApplicationVO> getById(Long id) {
        log.info("查询请假申请详情，id：{}", id);
        LeaveApplication entity = leaveApplicationMapper.selectById(id);
        if (entity == null) {
            return Result.error("请假申请不存在，id = " + id);
        }
        return Result.success(leaveApplicationConvert.entityToVo(entity));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<Long> add(LeaveApplicationDTO dto) {
        log.info("新增请假申请，参数：{}", dto);
        if (!StringUtils.hasText(dto.getApplicant())) {
            return Result.error("申请人不能为空");
        }
        if (!StringUtils.hasText(dto.getLeaveType())) {
            return Result.error("请假类型不能为空");
        }
        if (dto.getStartDate() == null) {
            return Result.error("开始日期不能为空");
        }
        if (dto.getEndDate() == null) {
            return Result.error("结束日期不能为空");
        }
        if (StringUtils.hasText(dto.getLeaveType()) && !isValidLeaveType(dto.getLeaveType())) {
            return Result.error("请假类型取值不合法，仅支持 annual / personal / sick / marriage / maternity / compensatory");
        }
        if (StringUtils.hasText(dto.getApprovalStatus()) && !isValidApprovalStatus(dto.getApprovalStatus())) {
            return Result.error("审批状态取值不合法，仅支持 pending / approved / rejected");
        }

        if (StringUtils.hasText(dto.getApprovalStatus())
                && !APPROVAL_STATUS_PENDING.equals(dto.getApprovalStatus())) {
            return Result.error("审批状态请通过「审批通过 / 驳回」操作变更");
        }

        if (dto.getStartDate() != null && dto.getEndDate() != null
                && dto.getEndDate().isBefore(dto.getStartDate())) {
            return Result.error("结束日期不能早于开始日期");
        }
        LeaveApplication entity = leaveApplicationConvert.dtoToEntity(dto);
        if (!StringUtils.hasText(entity.getApprovalStatus())) {
            entity.setApprovalStatus("pending");
        }
        if (!StringUtils.hasText(entity.getApplyNo())) {
            entity.setApplyNo(nextApplyNo());
        }
        entity.setApplyTime(LocalDateTime.now());
        entity.setLeaveDays(calcLeaveDays(entity.getStartDate(), entity.getEndDate()));
        entity.setCreateBy(currentOperator());
        leaveApplicationMapper.insert(entity);
        log.info("新增请假申请成功，id = {}", entity.getId());
        return Result.success(entity.getId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<Long> update(Long id, LeaveApplicationDTO dto) {
        log.info("编辑请假申请，id：{}，参数：{}", id, dto);
        LeaveApplication exist = leaveApplicationMapper.selectById(id);
        if (exist == null) {
            return Result.error("请假申请不存在，id = " + id);
        }
        if (dto.getApplicant() != null && !StringUtils.hasText(dto.getApplicant())) {
            return Result.error("申请人不能为空");
        }
        if (dto.getLeaveType() != null && !StringUtils.hasText(dto.getLeaveType())) {
            return Result.error("请假类型不能为空");
        }
        if (StringUtils.hasText(dto.getLeaveType()) && !isValidLeaveType(dto.getLeaveType())) {
            return Result.error("请假类型取值不合法，仅支持 annual / personal / sick / marriage / maternity / compensatory");
        }
        if (StringUtils.hasText(dto.getApprovalStatus()) && !isValidApprovalStatus(dto.getApprovalStatus())) {
            return Result.error("审批状态取值不合法，仅支持 pending / approved / rejected");
        }
        LocalDate startDate = dto.getStartDate() != null ? dto.getStartDate() : exist.getStartDate();
        LocalDate endDate = dto.getEndDate() != null ? dto.getEndDate() : exist.getEndDate();
        if (startDate != null && endDate != null && endDate.isBefore(startDate)) {
            return Result.error("结束日期不能早于开始日期");
        }
        dto.setLeaveDays(calcLeaveDays(startDate, endDate));
        LeaveApplication entity = leaveApplicationConvert.dtoToEntity(dto);
        entity.setId(id);
        entity.setUpdateBy(currentOperator());
        // updateById 仅更新非空字段：未传字段保留原值
        leaveApplicationMapper.updateById(entity);
        log.info("编辑请假申请成功，id = {}", id);
        return Result.success(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<Boolean> delete(Long id) {
        log.info("删除请假申请，id：{}", id);
        if (leaveApplicationMapper.selectById(id) == null) {
            return Result.error("请假申请不存在，id = " + id);
        }
        int rows = leaveApplicationMapper.deleteById(id);
        log.info("删除请假申请成功，id = {}", id);
        return Result.success(rows > 0);
    }

    /**
     * 校验 leaveType 取值合法性。
     */
    private boolean isValidLeaveType(String value) {
        return value == null
                || LEAVE_TYPE_ANNUAL.equals(value)
                || LEAVE_TYPE_PERSONAL.equals(value)
                || LEAVE_TYPE_SICK.equals(value)
                || LEAVE_TYPE_MARRIAGE.equals(value)
                || LEAVE_TYPE_MATERNITY.equals(value)
                || LEAVE_TYPE_COMPENSATORY.equals(value);
    }

    /**
     * 校验 approvalStatus 取值合法性。
     */
    private boolean isValidApprovalStatus(String value) {
        return value == null
                || APPROVAL_STATUS_PENDING.equals(value)
                || APPROVAL_STATUS_APPROVED.equals(value)
                || APPROVAL_STATUS_REJECTED.equals(value);
    }

    /**
     * 生成申请单号：前缀-yyyyMMdd-4位流水（按当天已有条数递增）。
     */
    private String nextApplyNo() {
        String prefix = "QJ-" + LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd")) + "-";
        Long todayCount = leaveApplicationMapper.selectCount(Wrappers.<LeaveApplication>lambdaQuery()
                .likeRight(LeaveApplication::getApplyNo, prefix));
        long seq = (todayCount == null ? 0L : todayCount) + 1;
        return prefix + String.format("%04d", seq);
    }

    /**
     * 请假天数 = 结束日期 - 开始日期 + 1（含首尾）。任一端为空或区间倒置时返回 null。
     */
    private BigDecimal calcLeaveDays(LocalDate startDate, LocalDate endDate) {
        if (startDate == null || endDate == null || endDate.isBefore(startDate)) {
            return null;
        }
        return BigDecimal.valueOf(ChronoUnit.DAYS.between(startDate, endDate) + 1);
    }

    /**
     * 审批流转：仅未审批状态可流转，同时记录审批人 / 审批意见 / 审批时间。
     */
    private Result<Boolean> changeApprovalStatus(Long id, String targetStatus, String opinion) {
        LeaveApplication exist = leaveApplicationMapper.selectById(id);
        if (exist == null) {
            return Result.error("请假申请不存在，id = " + id);
        }
        if (!APPROVAL_STATUS_PENDING.equals(exist.getApprovalStatus())) {
            return Result.error("该请假申请已审批，无需重复审批");
        }
        leaveApplicationMapper.update(null, Wrappers.<LeaveApplication>lambdaUpdate()
                .eq(LeaveApplication::getId, id)
                .set(LeaveApplication::getApprovalStatus, targetStatus)
                .set(LeaveApplication::getApprover, currentOperator())
                .set(LeaveApplication::getApprovalOpinion, opinion)
                .set(LeaveApplication::getApprovalTime, LocalDateTime.now())
                .set(LeaveApplication::getUpdateBy, currentOperator()));
        return Result.success(true);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<Boolean> approve(Long id, String opinion) {
        log.info("审批通过请假申请，id：{}，审批意见：{}", id, opinion);
        return changeApprovalStatus(id, APPROVAL_STATUS_APPROVED, opinion);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<Boolean> reject(Long id, String opinion) {
        if (!StringUtils.hasText(opinion)) {
            return Result.error("驳回原因不能为空");
        }
        log.info("驳回请假申请，id：{}，驳回原因：{}", id, opinion);
        return changeApprovalStatus(id, APPROVAL_STATUS_REJECTED, opinion);
    }

    /**
     * 当前操作人：取登录上下文中的登录名；未登录（如本地直连调试）时返回 null。
     */
    private String currentOperator() {
        if (UserContext.getUser() == null) {
            return null;
        }
        return UserContext.getUser().getUsername();
    }
}
