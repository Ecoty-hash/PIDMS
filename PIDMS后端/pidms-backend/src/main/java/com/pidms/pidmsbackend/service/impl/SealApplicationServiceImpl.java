package com.pidms.pidmsbackend.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.pidms.pidmsbackend.common.Result;
import com.pidms.pidmsbackend.common.UserContext;
import com.pidms.pidmsbackend.convert.SealApplicationConvert;
import com.pidms.pidmsbackend.dto.SealApplicationDTO;
import com.pidms.pidmsbackend.entity.PageInfo;
import com.pidms.pidmsbackend.entity.Project;
import com.pidms.pidmsbackend.entity.SealApplication;
import com.pidms.pidmsbackend.entity.SealApplicationQueryParam;
import com.pidms.pidmsbackend.entity.SealType;
import com.pidms.pidmsbackend.mapper.ProjectMapper;
import com.pidms.pidmsbackend.mapper.SealApplicationMapper;
import com.pidms.pidmsbackend.mapper.SealTypeMapper;
import com.pidms.pidmsbackend.service.SealApplicationService;
import com.pidms.pidmsbackend.vo.SealApplicationVO;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 用印申请服务
 * <p>项目与印章类型按「名称」提交/筛选：前端传 projectName / sealTypeName，
 * 后端在 pm_project / pm_seal_type 中解析为 project_id / seal_type_id 后落库与过滤。</p>
 * 审批状态：pending待审批 / approved已审批 / rejected已拒绝；审批流转走专用动作接口。
 */
@Slf4j
@Service
public class SealApplicationServiceImpl implements SealApplicationService {

    /** 审批状态：待审批 */
    private static final String APPROVAL_PENDING = "pending";
    /** 审批状态：已审批 */
    private static final String APPROVAL_APPROVED = "approved";
    /** 审批状态：已拒绝 */
    private static final String APPROVAL_REJECTED = "rejected";

    @Resource
    private SealApplicationMapper sealApplicationMapper;
    @Resource
    private ProjectMapper projectMapper;
    @Resource
    private SealTypeMapper sealTypeMapper;
    @Resource
    private SealApplicationConvert sealApplicationConvert;

    @Override
    public Result<PageInfo<SealApplicationVO>> pageQuery(SealApplicationQueryParam queryParam) {
        long pageNum = queryParam.getPage() == null ? 1 : queryParam.getPage();
        long pageSize = queryParam.getPageSize() == null ? 20 : queryParam.getPageSize();
        log.info("分页查询用印申请：pageNum = {}, pageSize = {}, 参数 = {}", pageNum, pageSize, queryParam);

        QueryWrapper<SealApplication> queryWrapper = new QueryWrapper<>();

        // 项目名称筛选：先在项目表模糊匹配，再按 project_id 过滤
        if (StringUtils.hasText(queryParam.getProjectName())) {
            List<Long> matchedProjectIds = projectMapper.selectList(Wrappers.<Project>lambdaQuery()
                            .like(Project::getProjectName, queryParam.getProjectName())
                            .select(Project::getId))
                    .stream().map(Project::getId).toList();
            queryWrapper.in("project_id", matchedProjectIds.isEmpty() ? List.of(-1L) : matchedProjectIds);
        }
        // 印章类型名称筛选：先在印章类型表模糊匹配，再按 seal_type_id 过滤
        if (StringUtils.hasText(queryParam.getSealTypeName())) {
            List<Long> matchedTypeIds = sealTypeMapper.selectList(Wrappers.<SealType>lambdaQuery()
                            .like(SealType::getTypeName, queryParam.getSealTypeName())
                            .select(SealType::getId))
                    .stream().map(SealType::getId).toList();
            queryWrapper.in("seal_type_id", matchedTypeIds.isEmpty() ? List.of(-1L) : matchedTypeIds);
        }

        queryWrapper.like(StringUtils.hasText(queryParam.getTitle()), "title", queryParam.getTitle());
        queryWrapper.eq(StringUtils.hasText(queryParam.getApprovalStatus()), "approval_status", queryParam.getApprovalStatus());
        queryWrapper.like(StringUtils.hasText(queryParam.getSealDepartment()), "seal_department", queryParam.getSealDepartment());
        queryWrapper.eq(StringUtils.hasText(queryParam.getSealMethod()), "seal_method", queryParam.getSealMethod());
        queryWrapper.like(StringUtils.hasText(queryParam.getApplicant()), "applicant", queryParam.getApplicant());
        // 申请日期精确到天
        if (queryParam.getApplyDate() != null) {
            queryWrapper.apply("date(apply_date) = {0}", queryParam.getApplyDate());
        }
        // 关键字：单号 / 标题 / 用印文件名称 / 备注
        if (StringUtils.hasText(queryParam.getKeyword())) {
            queryWrapper.nested(wrapper -> wrapper
                    .like("bill_no", queryParam.getKeyword())
                    .or()
                    .like("title", queryParam.getKeyword())
                    .or()
                    .like("seal_file_name", queryParam.getKeyword())
                    .or()
                    .like("remark", queryParam.getKeyword())
            );
        }
        queryWrapper.orderByDesc("id");

        Page<SealApplication> page = new Page<>(pageNum, pageSize);
        Page<SealApplication> resultPage = sealApplicationMapper.selectPage(page, queryWrapper);
        List<SealApplication> records = resultPage.getRecords();
        log.info("分页查询用印申请结果条数：{}", records.size());

        PageInfo<SealApplicationVO> pageInfo = new PageInfo<>();
        pageInfo.setPage((int) pageNum);
        pageInfo.setPageSize((int) pageSize);
        pageInfo.setTotal(resultPage.getTotal());
        pageInfo.setPages((int) ((resultPage.getTotal() + pageSize - 1) / pageSize));

        if (records.isEmpty()) {
            pageInfo.setList(List.of());
            return Result.success(pageInfo);
        }

        // 批量带出项目名称与印章类型名称
        Map<Long, String> projectNameMap = loadProjectNames(records.stream()
                .map(SealApplication::getProjectId).filter(Objects::nonNull).collect(Collectors.toSet()));
        Map<Long, String> typeNameMap = loadSealTypeNames(records.stream()
                .map(SealApplication::getSealTypeId).filter(Objects::nonNull).collect(Collectors.toSet()));

        List<SealApplicationVO> voList = records.stream()
                .map(sealApplicationConvert::entityToVo)
                .peek(vo -> {
                    vo.setProjectName(projectNameMap.get(vo.getProjectId()));
                    vo.setSealTypeName(typeNameMap.get(vo.getSealTypeId()));
                })
                .toList();

        pageInfo.setList(voList);
        return Result.success(pageInfo);
    }

    @Override
    public Result<SealApplicationVO> getById(Long id) {
        SealApplication entity = sealApplicationMapper.selectById(id);
        if (entity == null) {
            return Result.error("用印申请不存在，id = " + id);
        }
        SealApplicationVO vo = sealApplicationConvert.entityToVo(entity);
        if (entity.getProjectId() != null) {
            Project project = projectMapper.selectById(entity.getProjectId());
            vo.setProjectName(project == null ? null : project.getProjectName());
        }
        if (entity.getSealTypeId() != null) {
            SealType sealType = sealTypeMapper.selectById(entity.getSealTypeId());
            vo.setSealTypeName(sealType == null ? null : sealType.getTypeName());
        }
        return Result.success(vo);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<Long> add(SealApplicationDTO dto) {
        if (!StringUtils.hasText(dto.getProjectName())) {
            return Result.error("项目不能为空");
        }
        if (!StringUtils.hasText(dto.getSealTypeName())) {
            return Result.error("印章类型不能为空");
        }
        if (!StringUtils.hasText(dto.getTitle())) {
            return Result.error("标题不能为空");
        }
        // 按名称解析项目 / 印章类型
        Project project = resolveProject(dto.getProjectName());
        if (project == null) {
            return Result.error("项目名称不存在：" + dto.getProjectName());
        }
        SealType sealType = resolveSealType(dto.getSealTypeName());
        if (sealType == null) {
            return Result.error("印章类型名称不存在：" + dto.getSealTypeName());
        }

        SealApplication entity = sealApplicationConvert.dtoToEntity(dto);
        entity.setProjectId(project.getId());
        entity.setSealTypeId(sealType.getId());
        // 单号：显式传值需唯一；留空自动生成
        if (StringUtils.hasText(dto.getBillNo())) {
            Long dupCount = sealApplicationMapper.selectCount(Wrappers.<SealApplication>lambdaQuery()
                    .eq(SealApplication::getBillNo, dto.getBillNo()));
            if (dupCount != null && dupCount > 0) {
                return Result.error("单号已存在：" + dto.getBillNo());
            }
            entity.setBillNo(dto.getBillNo().trim());
        } else {
            entity.setBillNo(generateBillNo());
        }
        // 文件份数未传默认 1
        if (entity.getFileCopies() == null) {
            entity.setFileCopies(1);
        }
        // 新增均为待审批
        entity.setApprovalStatus(APPROVAL_PENDING);
        entity.setCreateBy(currentOperator());
        sealApplicationMapper.insert(entity);
        log.info("新增用印申请成功，id = {}，单号 = {}", entity.getId(), entity.getBillNo());
        return Result.success(entity.getId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<Long> update(Long id, SealApplicationDTO dto) {
        SealApplication exist = sealApplicationMapper.selectById(id);
        if (exist == null) {
            return Result.error("用印申请不存在，id = " + id);
        }
        // 审批已完成（已审批/已拒绝）的记录不允许再编辑
        if (APPROVAL_APPROVED.equals(exist.getApprovalStatus()) || APPROVAL_REJECTED.equals(exist.getApprovalStatus())) {
            return Result.error("该用印申请已审批/已拒绝，不可编辑");
        }

        // 项目名称/印章类型名称解析：传了才重新解析，否则保留原值
        Long projectId = exist.getProjectId();
        if (StringUtils.hasText(dto.getProjectName())) {
            Project project = resolveProject(dto.getProjectName());
            if (project == null) {
                return Result.error("项目名称不存在：" + dto.getProjectName());
            }
            projectId = project.getId();
        }
        Long sealTypeId = exist.getSealTypeId();
        if (StringUtils.hasText(dto.getSealTypeName())) {
            SealType sealType = resolveSealType(dto.getSealTypeName());
            if (sealType == null) {
                return Result.error("印章类型名称不存在：" + dto.getSealTypeName());
            }
            sealTypeId = sealType.getId();
        }

        // 单号：传了才改并校验唯一；留空保留原单号（不重排）
        String billNo = exist.getBillNo();
        if (StringUtils.hasText(dto.getBillNo())) {
            Long dupCount = sealApplicationMapper.selectCount(Wrappers.<SealApplication>lambdaQuery()
                    .eq(SealApplication::getBillNo, dto.getBillNo())
                    .ne(SealApplication::getId, id));
            if (dupCount != null && dupCount > 0) {
                return Result.error("单号已存在：" + dto.getBillNo());
            }
            billNo = dto.getBillNo().trim();
        }

        Integer fileCopies = dto.getFileCopies() == null ? exist.getFileCopies() : dto.getFileCopies();

        // 显式 SET 可编辑字段；审批状态 / 审批人不在此变更（走 approve/reject）
        var updateWrapper = Wrappers.<SealApplication>lambdaUpdate()
                .eq(SealApplication::getId, id)
                .set(SealApplication::getProjectId, projectId)
                .set(SealApplication::getSealTypeId, sealTypeId)
                .set(SealApplication::getTitle, StringUtils.hasText(dto.getTitle()) ? dto.getTitle() : exist.getTitle())
                .set(SealApplication::getBillNo, billNo)
                .set(SealApplication::getApplyDate, dto.getApplyDate() == null ? exist.getApplyDate() : dto.getApplyDate())
                .set(SealApplication::getApplicant, StringUtils.hasText(dto.getApplicant()) ? dto.getApplicant() : exist.getApplicant())
                .set(SealApplication::getSealDepartment, StringUtils.hasText(dto.getSealDepartment()) ? dto.getSealDepartment() : exist.getSealDepartment())
                .set(SealApplication::getSealFileName, StringUtils.hasText(dto.getSealFileName()) ? dto.getSealFileName() : exist.getSealFileName())
                .set(SealApplication::getFileCopies, fileCopies == null ? 1 : fileCopies)
                .set(SealApplication::getSealMethod, StringUtils.hasText(dto.getSealMethod()) ? dto.getSealMethod() : exist.getSealMethod())
                .set(SealApplication::getSealDescription, dto.getSealDescription())
                .set(SealApplication::getRemark, dto.getRemark())
                .set(SealApplication::getUpdateBy, currentOperator());
        sealApplicationMapper.update(null, updateWrapper);
        log.info("编辑用印申请成功，id = {}", id);
        return Result.success(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<Boolean> delete(Long id) {
        if (sealApplicationMapper.selectById(id) == null) {
            return Result.error("用印申请不存在，id = " + id);
        }
        int rows = sealApplicationMapper.deleteById(id);
        log.info("删除用印申请成功，id = {}", id);
        return Result.success(rows > 0);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<Boolean> approve(Long id) {
        SealApplication exist = sealApplicationMapper.selectById(id);
        if (exist == null) {
            return Result.error("用印申请不存在，id = " + id);
        }
        if (!APPROVAL_PENDING.equals(exist.getApprovalStatus())) {
            return Result.error("仅待审批的用印申请可审批通过，当前状态：" + exist.getApprovalStatus());
        }
        sealApplicationMapper.update(null, Wrappers.<SealApplication>lambdaUpdate()
                .eq(SealApplication::getId, id)
                .set(SealApplication::getApprovalStatus, APPROVAL_APPROVED)
                .set(SealApplication::getApprover, currentOperator())
                .set(SealApplication::getUpdateBy, currentOperator()));
        log.info("审批通过用印申请成功，id = {}", id);
        return Result.success(true);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<Boolean> reject(Long id) {
        SealApplication exist = sealApplicationMapper.selectById(id);
        if (exist == null) {
            return Result.error("用印申请不存在，id = " + id);
        }
        if (!APPROVAL_PENDING.equals(exist.getApprovalStatus())) {
            return Result.error("仅待审批的用印申请可驳回，当前状态：" + exist.getApprovalStatus());
        }
        sealApplicationMapper.update(null, Wrappers.<SealApplication>lambdaUpdate()
                .eq(SealApplication::getId, id)
                .set(SealApplication::getApprovalStatus, APPROVAL_REJECTED)
                .set(SealApplication::getApprover, currentOperator())
                .set(SealApplication::getUpdateBy, currentOperator()));
        log.info("驳回用印申请成功，id = {}", id);
        return Result.success(true);
    }

    // ---------- 私有工具 ----------

    /**
     * 按项目名称精确查找项目（取第一条，避免同名项目导致 TooManyResults）
     */
    private Project resolveProject(String projectName) {
        if (!StringUtils.hasText(projectName)) {
            return null;
        }
        return projectMapper.selectOne(Wrappers.<Project>lambdaQuery()
                .eq(Project::getProjectName, projectName)
                .last("LIMIT 1"));
    }

    /**
     * 按印章类型名称精确查找类型（取第一条）
     */
    private SealType resolveSealType(String typeName) {
        if (!StringUtils.hasText(typeName)) {
            return null;
        }
        return sealTypeMapper.selectOne(Wrappers.<SealType>lambdaQuery()
                .eq(SealType::getTypeName, typeName)
                .last("LIMIT 1"));
    }

    private Map<Long, String> loadProjectNames(Set<Long> ids) {
        Map<Long, String> map = new HashMap<>();
        if (!ids.isEmpty()) {
            projectMapper.selectBatchIds(ids)
                    .forEach(project -> map.put(project.getId(), project.getProjectName()));
        }
        return map;
    }

    private Map<Long, String> loadSealTypeNames(Set<Long> ids) {
        Map<Long, String> map = new HashMap<>();
        if (!ids.isEmpty()) {
            sealTypeMapper.selectBatchIds(ids)
                    .forEach(type -> map.put(type.getId(), type.getTypeName()));
        }
        return map;
    }

    /**
     * 自动生成单号，格式：YY-年份-4位流水（如 YY-2026-0001），
     * 按当天已有最大流水 +1（与种子数据 YY-2026-0001 格式一致）。
     */
    private String generateBillNo() {
        String prefix = "YY-" + LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy")) + "-";
        List<SealApplication> existList = sealApplicationMapper.selectList(Wrappers.<SealApplication>lambdaQuery()
                .likeRight(SealApplication::getBillNo, prefix)
                .select(SealApplication::getBillNo));
        int currentMaxSerial = 0;
        for (SealApplication item : existList) {
            String billNo = item.getBillNo();
            if (StrUtil.isBlank(billNo) || billNo.length() < prefix.length() + 4) {
                continue;
            }
            String serialStr = billNo.substring(billNo.length() - 4);
            try {
                int serialNum = Integer.parseInt(serialStr);
                if (serialNum > currentMaxSerial) {
                    currentMaxSerial = serialNum;
                }
            } catch (NumberFormatException e) {
                log.error("单号 {} 末尾流水号解析失败", billNo);
            }
        }
        return prefix + String.format("%04d", currentMaxSerial + 1);
    }

    private String currentOperator() {
        if (UserContext.getUser() == null) {
            return null;
        }
        return UserContext.getUser().getUsername();
    }
}
