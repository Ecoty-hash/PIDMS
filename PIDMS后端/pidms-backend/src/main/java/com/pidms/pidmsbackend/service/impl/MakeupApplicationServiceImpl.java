package com.pidms.pidmsbackend.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.pidms.pidmsbackend.common.Result;
import com.pidms.pidmsbackend.common.UserContext;
import com.pidms.pidmsbackend.convert.MakeupApplicationConvert;
import com.pidms.pidmsbackend.dto.MakeupApplicationDTO;
import com.pidms.pidmsbackend.entity.MakeupApplication;
import com.pidms.pidmsbackend.entity.MakeupApplicationQueryParam;
import com.pidms.pidmsbackend.entity.PageInfo;
import com.pidms.pidmsbackend.mapper.MakeupApplicationMapper;
import com.pidms.pidmsbackend.service.MakeupApplicationService;
import com.pidms.pidmsbackend.vo.MakeupApplicationVO;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * 补卡申请 服务实现
 * <p>审批状态：pending未审批 / approved已通过 / rejected已驳回，流转走 /approve、/reject 动作接口。</p>
 */
@Slf4j
@Service
public class MakeupApplicationServiceImpl implements MakeupApplicationService {

    /** cardMissType：on-duty */
    private static final String CARD_MISS_TYPE_ON_DUTY = "on-duty";
    /** cardMissType：off-duty */
    private static final String CARD_MISS_TYPE_OFF_DUTY = "off-duty";
    /** cardMissType：whole-day */
    private static final String CARD_MISS_TYPE_WHOLE_DAY = "whole-day";

    /** approvalStatus：pending */
    private static final String APPROVAL_STATUS_PENDING = "pending";
    /** approvalStatus：approved */
    private static final String APPROVAL_STATUS_APPROVED = "approved";
    /** approvalStatus：rejected */
    private static final String APPROVAL_STATUS_REJECTED = "rejected";

    @Resource
    private MakeupApplicationMapper makeupApplicationMapper;

    @Resource
    private MakeupApplicationConvert makeupApplicationConvert;

    @Override
    public Result<PageInfo<MakeupApplicationVO>> pageQuery(MakeupApplicationQueryParam queryParam) {
        long pageNum = queryParam.getPage() == null ? 1 : queryParam.getPage();
        long pageSize = queryParam.getPageSize() == null ? 20 : queryParam.getPageSize();
        log.info("分页查询补卡申请：pageNum = {}, pageSize = {}, 参数 = {}", pageNum, pageSize, queryParam);

        QueryWrapper<MakeupApplication> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq(StringUtils.hasText(queryParam.getApplyNo()), "apply_no", queryParam.getApplyNo());
        queryWrapper.eq(StringUtils.hasText(queryParam.getApplicant()), "applicant", queryParam.getApplicant());
        queryWrapper.eq(StringUtils.hasText(queryParam.getCardMissType()), "card_miss_type", queryParam.getCardMissType());
        queryWrapper.eq(StringUtils.hasText(queryParam.getApprovalStatus()), "approval_status", queryParam.getApprovalStatus());
        queryWrapper.ge(queryParam.getStartDate() != null, "start_date", queryParam.getStartDate());
        queryWrapper.le(queryParam.getEndDate() != null, "end_date", queryParam.getEndDate());

        if (StringUtils.hasText(queryParam.getKeyword())) {
            String keyword = queryParam.getKeyword().trim();
            queryWrapper.and(w -> w
                        .like("apply_no", keyword)
                        .or().like("applicant", keyword)
                        .or().like("witness", keyword));
        }
        queryWrapper.orderByAsc("id");

        Page<MakeupApplication> page = new Page<>(pageNum, pageSize);
        Page<MakeupApplication> resultPage = makeupApplicationMapper.selectPage(page, queryWrapper);
        List<MakeupApplication> records = resultPage.getRecords();
        log.info("分页查询补卡申请结果条数：{}", records.size());

        PageInfo<MakeupApplicationVO> pageInfo = new PageInfo<>();
        pageInfo.setPage((int) resultPage.getCurrent());
        pageInfo.setPageSize((int) resultPage.getSize());
        pageInfo.setTotal(resultPage.getTotal());
        pageInfo.setPages((int) ((resultPage.getTotal() + pageSize - 1) / pageSize));
        pageInfo.setList(makeupApplicationConvert.entityListToVoList(records));
        return Result.success(pageInfo);
    }

    @Override
    public Result<MakeupApplicationVO> getById(Long id) {
        log.info("查询补卡申请详情，id：{}", id);
        MakeupApplication entity = makeupApplicationMapper.selectById(id);
        if (entity == null) {
            return Result.error("补卡申请不存在，id = " + id);
        }
        return Result.success(makeupApplicationConvert.entityToVo(entity));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<Long> add(MakeupApplicationDTO dto) {
        log.info("新增补卡申请，参数：{}", dto);
        if (!StringUtils.hasText(dto.getApplicant())) {
            return Result.error("申请人不能为空");
        }
        if (dto.getCardMissDate() == null) {
            return Result.error("缺卡日期不能为空");
        }
        if (!StringUtils.hasText(dto.getCardMissType())) {
            return Result.error("缺卡类型不能为空");
        }
        if (StringUtils.hasText(dto.getCardMissType()) && !isValidCardMissType(dto.getCardMissType())) {
            return Result.error("缺卡类型取值不合法，仅支持 on-duty / off-duty / whole-day");
        }
        if (StringUtils.hasText(dto.getApprovalStatus()) && !isValidApprovalStatus(dto.getApprovalStatus())) {
            return Result.error("审批状态取值不合法，仅支持 pending / approved / rejected");
        }

        if (StringUtils.hasText(dto.getApprovalStatus())
                && !APPROVAL_STATUS_PENDING.equals(dto.getApprovalStatus())) {
            return Result.error("审批状态请通过「审批通过 / 驳回」操作变更");
        }
        MakeupApplication entity = makeupApplicationConvert.dtoToEntity(dto);
        if (!StringUtils.hasText(entity.getApprovalStatus())) {
            entity.setApprovalStatus("pending");
        }
        if (!StringUtils.hasText(entity.getApplyNo())) {
            entity.setApplyNo(nextApplyNo());
        }
        entity.setApplyTime(LocalDateTime.now());
        entity.setCreateBy(currentOperator());
        makeupApplicationMapper.insert(entity);
        log.info("新增补卡申请成功，id = {}", entity.getId());
        return Result.success(entity.getId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<Long> update(Long id, MakeupApplicationDTO dto) {
        log.info("编辑补卡申请，id：{}，参数：{}", id, dto);
        MakeupApplication exist = makeupApplicationMapper.selectById(id);
        if (exist == null) {
            return Result.error("补卡申请不存在，id = " + id);
        }
        if (dto.getApplicant() != null && !StringUtils.hasText(dto.getApplicant())) {
            return Result.error("申请人不能为空");
        }
        if (dto.getCardMissType() != null && !StringUtils.hasText(dto.getCardMissType())) {
            return Result.error("缺卡类型不能为空");
        }
        if (StringUtils.hasText(dto.getCardMissType()) && !isValidCardMissType(dto.getCardMissType())) {
            return Result.error("缺卡类型取值不合法，仅支持 on-duty / off-duty / whole-day");
        }
        if (StringUtils.hasText(dto.getApprovalStatus()) && !isValidApprovalStatus(dto.getApprovalStatus())) {
            return Result.error("审批状态取值不合法，仅支持 pending / approved / rejected");
        }

        MakeupApplication entity = makeupApplicationConvert.dtoToEntity(dto);
        entity.setId(id);
        entity.setUpdateBy(currentOperator());
        // updateById 仅更新非空字段：未传字段保留原值
        makeupApplicationMapper.updateById(entity);
        log.info("编辑补卡申请成功，id = {}", id);
        return Result.success(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<Boolean> delete(Long id) {
        log.info("删除补卡申请，id：{}", id);
        if (makeupApplicationMapper.selectById(id) == null) {
            return Result.error("补卡申请不存在，id = " + id);
        }
        int rows = makeupApplicationMapper.deleteById(id);
        log.info("删除补卡申请成功，id = {}", id);
        return Result.success(rows > 0);
    }

    /**
     * 校验 cardMissType 取值合法性。
     */
    private boolean isValidCardMissType(String value) {
        return value == null
                || CARD_MISS_TYPE_ON_DUTY.equals(value)
                || CARD_MISS_TYPE_OFF_DUTY.equals(value)
                || CARD_MISS_TYPE_WHOLE_DAY.equals(value);
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
        String prefix = "BK-" + LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd")) + "-";
        Long todayCount = makeupApplicationMapper.selectCount(Wrappers.<MakeupApplication>lambdaQuery()
                .likeRight(MakeupApplication::getApplyNo, prefix));
        long seq = (todayCount == null ? 0L : todayCount) + 1;
        return prefix + String.format("%04d", seq);
    }

    /**
     * 审批流转：仅未审批状态可流转，同时记录审批人 / 审批意见 / 审批时间。
     */
    private Result<Boolean> changeApprovalStatus(Long id, String targetStatus, String opinion) {
        MakeupApplication exist = makeupApplicationMapper.selectById(id);
        if (exist == null) {
            return Result.error("补卡申请不存在，id = " + id);
        }
        if (!APPROVAL_STATUS_PENDING.equals(exist.getApprovalStatus())) {
            return Result.error("该补卡申请已审批，无需重复审批");
        }
        makeupApplicationMapper.update(null, Wrappers.<MakeupApplication>lambdaUpdate()
                .eq(MakeupApplication::getId, id)
                .set(MakeupApplication::getApprovalStatus, targetStatus)
                .set(MakeupApplication::getApprover, currentOperator())
                .set(MakeupApplication::getApprovalOpinion, opinion)
                .set(MakeupApplication::getApprovalTime, LocalDateTime.now())
                .set(MakeupApplication::getUpdateBy, currentOperator()));
        return Result.success(true);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<Boolean> approve(Long id, String opinion) {
        log.info("审批通过补卡申请，id：{}，审批意见：{}", id, opinion);
        return changeApprovalStatus(id, APPROVAL_STATUS_APPROVED, opinion);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<Boolean> reject(Long id, String opinion) {
        if (!StringUtils.hasText(opinion)) {
            return Result.error("驳回原因不能为空");
        }
        log.info("驳回补卡申请，id：{}，驳回原因：{}", id, opinion);
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
