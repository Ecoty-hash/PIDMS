package com.pidms.pidmsbackend.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.pidms.pidmsbackend.common.Result;
import com.pidms.pidmsbackend.common.UserContext;
import com.pidms.pidmsbackend.convert.BusinessTripConvert;
import com.pidms.pidmsbackend.dto.BusinessTripDTO;
import com.pidms.pidmsbackend.entity.BusinessTrip;
import com.pidms.pidmsbackend.entity.BusinessTripQueryParam;
import com.pidms.pidmsbackend.entity.PageInfo;
import com.pidms.pidmsbackend.entity.Project;
import com.pidms.pidmsbackend.mapper.BusinessTripMapper;
import com.pidms.pidmsbackend.mapper.ProjectMapper;
import com.pidms.pidmsbackend.service.BusinessTripService;
import com.pidms.pidmsbackend.vo.BusinessTripVO;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.util.CollectionUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 出差申请 服务实现
 * <p>审批状态：pending未审批 / approved已通过 / rejected已驳回，流转走 /approve、/reject 动作接口。</p>
 */
@Slf4j
@Service
public class BusinessTripServiceImpl implements BusinessTripService {

    /** approvalStatus：pending */
    private static final String APPROVAL_STATUS_PENDING = "pending";
    /** approvalStatus：approved */
    private static final String APPROVAL_STATUS_APPROVED = "approved";
    /** approvalStatus：rejected */
    private static final String APPROVAL_STATUS_REJECTED = "rejected";

    @Resource
    private BusinessTripMapper businessTripMapper;

    @Resource
    private ProjectMapper projectMapper;

    @Resource
    private BusinessTripConvert businessTripConvert;

    @Override
    public Result<PageInfo<BusinessTripVO>> pageQuery(BusinessTripQueryParam queryParam) {
        long pageNum = queryParam.getPage() == null ? 1 : queryParam.getPage();
        long pageSize = queryParam.getPageSize() == null ? 20 : queryParam.getPageSize();
        log.info("分页查询出差申请：pageNum = {}, pageSize = {}, 参数 = {}", pageNum, pageSize, queryParam);

        QueryWrapper<BusinessTrip> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq(StringUtils.hasText(queryParam.getApplyNo()), "apply_no", queryParam.getApplyNo());
        queryWrapper.eq(StringUtils.hasText(queryParam.getApplicant()), "applicant", queryParam.getApplicant());
        queryWrapper.eq(StringUtils.hasText(queryParam.getTripDestination()), "trip_destination", queryParam.getTripDestination());
        queryWrapper.eq(StringUtils.hasText(queryParam.getApprovalStatus()), "approval_status", queryParam.getApprovalStatus());
        queryWrapper.ge(queryParam.getStartDate() != null, "start_date", queryParam.getStartDate());
        queryWrapper.le(queryParam.getEndDate() != null, "end_date", queryParam.getEndDate());

        if (StringUtils.hasText(queryParam.getKeyword())) {
            String keyword = queryParam.getKeyword().trim();
            queryWrapper.and(w -> w
                        .like("apply_no", keyword)
                        .or().like("applicant", keyword)
                        .or().like("trip_destination", keyword));
        }
        queryWrapper.orderByAsc("id");

        Page<BusinessTrip> page = new Page<>(pageNum, pageSize);
        Page<BusinessTrip> resultPage = businessTripMapper.selectPage(page, queryWrapper);
        List<BusinessTrip> records = resultPage.getRecords();
        log.info("分页查询出差申请结果条数：{}", records.size());

        PageInfo<BusinessTripVO> pageInfo = new PageInfo<>();
        pageInfo.setPage((int) resultPage.getCurrent());
        pageInfo.setPageSize((int) resultPage.getSize());
        pageInfo.setTotal(resultPage.getTotal());
        pageInfo.setPages((int) ((resultPage.getTotal() + pageSize - 1) / pageSize));
        pageInfo.setList(fillDisplayFields(businessTripConvert.entityListToVoList(records)));
        return Result.success(pageInfo);
    }

    @Override
    public Result<BusinessTripVO> getById(Long id) {
        log.info("查询出差申请详情，id：{}", id);
        BusinessTrip entity = businessTripMapper.selectById(id);
        if (entity == null) {
            return Result.error("出差申请不存在，id = " + id);
        }
        return Result.success(fillDisplayFields(businessTripConvert.entityToVo(entity)));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<Long> add(BusinessTripDTO dto) {
        log.info("新增出差申请，参数：{}", dto);
        if (!StringUtils.hasText(dto.getApplicant())) {
            return Result.error("申请人不能为空");
        }
        if (!StringUtils.hasText(dto.getTripDestination())) {
            return Result.error("出差地点不能为空");
        }
        if (dto.getStartDate() == null) {
            return Result.error("开始日期不能为空");
        }
        if (dto.getEndDate() == null) {
            return Result.error("结束日期不能为空");
        }
        if (StringUtils.hasText(dto.getApprovalStatus()) && !isValidApprovalStatus(dto.getApprovalStatus())) {
            return Result.error("审批状态取值不合法，仅支持 pending / approved / rejected");
        }
        if (dto.getProjectId() != null && projectMapper.selectById(dto.getProjectId()) == null) {
            return Result.error("所属项目不存在，projectId = " + dto.getProjectId());
        }

        if (StringUtils.hasText(dto.getApprovalStatus())
                && !APPROVAL_STATUS_PENDING.equals(dto.getApprovalStatus())) {
            return Result.error("审批状态请通过「审批通过 / 驳回」操作变更");
        }
        BusinessTrip entity = businessTripConvert.dtoToEntity(dto);
        if (!StringUtils.hasText(entity.getApprovalStatus())) {
            entity.setApprovalStatus("pending");
        }
        if (!StringUtils.hasText(entity.getApplyNo())) {
            entity.setApplyNo(nextApplyNo());
        }
        entity.setApplyTime(LocalDateTime.now());
        entity.setCreateBy(currentOperator());
        businessTripMapper.insert(entity);
        log.info("新增出差申请成功，id = {}", entity.getId());
        return Result.success(entity.getId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<Long> update(Long id, BusinessTripDTO dto) {
        log.info("编辑出差申请，id：{}，参数：{}", id, dto);
        BusinessTrip exist = businessTripMapper.selectById(id);
        if (exist == null) {
            return Result.error("出差申请不存在，id = " + id);
        }
        if (dto.getApplicant() != null && !StringUtils.hasText(dto.getApplicant())) {
            return Result.error("申请人不能为空");
        }
        if (dto.getTripDestination() != null && !StringUtils.hasText(dto.getTripDestination())) {
            return Result.error("出差地点不能为空");
        }
        if (StringUtils.hasText(dto.getApprovalStatus()) && !isValidApprovalStatus(dto.getApprovalStatus())) {
            return Result.error("审批状态取值不合法，仅支持 pending / approved / rejected");
        }
        if (dto.getProjectId() != null && projectMapper.selectById(dto.getProjectId()) == null) {
            return Result.error("所属项目不存在，projectId = " + dto.getProjectId());
        }

        BusinessTrip entity = businessTripConvert.dtoToEntity(dto);
        entity.setId(id);
        entity.setUpdateBy(currentOperator());
        // updateById 仅更新非空字段：未传字段保留原值
        businessTripMapper.updateById(entity);
        log.info("编辑出差申请成功，id = {}", id);
        return Result.success(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<Boolean> delete(Long id) {
        log.info("删除出差申请，id：{}", id);
        if (businessTripMapper.selectById(id) == null) {
            return Result.error("出差申请不存在，id = " + id);
        }
        int rows = businessTripMapper.deleteById(id);
        log.info("删除出差申请成功，id = {}", id);
        return Result.success(rows > 0);
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
     * 回填 VO 展示字段（表中不落库，由 Service 联查 / 计算）。
     */
    private List<BusinessTripVO> fillDisplayFields(List<BusinessTripVO> voList) {
        if (CollectionUtils.isEmpty(voList)) {
            return voList;
        }

        List<Long> projectIds = voList.stream()
                .map(BusinessTripVO::getProjectId)
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());
        Map<Long, String> idToName = projectIds.isEmpty()
                ? Collections.emptyMap()
                : projectMapper.selectBatchIds(projectIds).stream()
                        .collect(Collectors.toMap(Project::getId, Project::getProjectName, (a, b) -> a));
        voList.forEach(vo -> vo.setProjectName(idToName.get(vo.getProjectId())));

        voList.forEach(vo -> {
            if (vo.getStartDate() == null && vo.getEndDate() == null) {
                vo.setTripDate(null);
            } else {
                vo.setTripDate((vo.getStartDate() == null ? "" : vo.getStartDate().toString())
                        + " ~ "
                        + (vo.getEndDate() == null ? "" : vo.getEndDate().toString()));
            }
        });
        return voList;
    }

    /**
     * 回填单个 VO 的展示字段。
     */
    private BusinessTripVO fillDisplayFields(BusinessTripVO vo) {
        if (vo == null) {
            return null;
        }
        fillDisplayFields(List.of(vo));
        return vo;
    }

    /**
     * 生成申请单号：前缀-yyyyMMdd-4位流水（按当天已有条数递增）。
     */
    private String nextApplyNo() {
        String prefix = "CC-" + LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd")) + "-";
        Long todayCount = businessTripMapper.selectCount(Wrappers.<BusinessTrip>lambdaQuery()
                .likeRight(BusinessTrip::getApplyNo, prefix));
        long seq = (todayCount == null ? 0L : todayCount) + 1;
        return prefix + String.format("%04d", seq);
    }

    /**
     * 审批流转：仅未审批状态可流转，同时记录审批人 / 审批意见 / 审批时间。
     */
    private Result<Boolean> changeApprovalStatus(Long id, String targetStatus, String opinion) {
        BusinessTrip exist = businessTripMapper.selectById(id);
        if (exist == null) {
            return Result.error("出差申请不存在，id = " + id);
        }
        if (!APPROVAL_STATUS_PENDING.equals(exist.getApprovalStatus())) {
            return Result.error("该出差申请已审批，无需重复审批");
        }
        businessTripMapper.update(null, Wrappers.<BusinessTrip>lambdaUpdate()
                .eq(BusinessTrip::getId, id)
                .set(BusinessTrip::getApprovalStatus, targetStatus)
                .set(BusinessTrip::getApprover, currentOperator())
                .set(BusinessTrip::getApprovalOpinion, opinion)
                .set(BusinessTrip::getApprovalTime, LocalDateTime.now())
                .set(BusinessTrip::getUpdateBy, currentOperator()));
        return Result.success(true);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<Boolean> approve(Long id, String opinion) {
        log.info("审批通过出差申请，id：{}，审批意见：{}", id, opinion);
        return changeApprovalStatus(id, APPROVAL_STATUS_APPROVED, opinion);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<Boolean> reject(Long id, String opinion) {
        if (!StringUtils.hasText(opinion)) {
            return Result.error("驳回原因不能为空");
        }
        log.info("驳回出差申请，id：{}，驳回原因：{}", id, opinion);
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
