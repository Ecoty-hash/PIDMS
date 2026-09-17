package com.pidms.pidmsbackend.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.pidms.pidmsbackend.common.Result;
import com.pidms.pidmsbackend.common.UserContext;
import com.pidms.pidmsbackend.convert.PersonnelConvert;
import com.pidms.pidmsbackend.dto.PersonnelDTO;
import com.pidms.pidmsbackend.entity.PageInfo;
import com.pidms.pidmsbackend.entity.PersonnelQueryParam;
import com.pidms.pidmsbackend.entity.SysOrg;
import com.pidms.pidmsbackend.entity.SysRole;
import com.pidms.pidmsbackend.entity.SysUser;
import com.pidms.pidmsbackend.entity.SysUserRole;
import com.pidms.pidmsbackend.mapper.SysOrgMapper;
import com.pidms.pidmsbackend.mapper.SysRoleMapper;
import com.pidms.pidmsbackend.mapper.SysUserMapper;
import com.pidms.pidmsbackend.mapper.SysUserRoleMapper;
import com.pidms.pidmsbackend.service.PersonnelService;
import com.pidms.pidmsbackend.vo.PersonnelVO;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 人员管理 服务实现
 * <p>落库表为 sys_user；角色分配落 sys_user_role（一人一角色，唯一键 user_id + role_id）。
 * 角色名称、机构名称不落库，由 Service 批量联查回填到 VO。</p>
 */
@Slf4j
@Service
public class PersonnelServiceImpl implements PersonnelService {

    /** 性别：男 */
    private static final String GENDER_MALE = "male";
    /** 性别：女 */
    private static final String GENDER_FEMALE = "female";
    /** 状态：在职 */
    private static final String STATUS_ENABLED = "enabled";
    /** 状态：离职 */
    private static final String STATUS_DISABLED = "disabled";

    @Resource
    private SysUserMapper sysUserMapper;

    @Resource
    private SysUserRoleMapper sysUserRoleMapper;

    @Resource
    private SysRoleMapper sysRoleMapper;

    @Resource
    private SysOrgMapper sysOrgMapper;

    @Resource
    private PersonnelConvert personnelConvert;

    @Override
    public Result<PageInfo<PersonnelVO>> pageQuery(PersonnelQueryParam queryParam) {
        long pageNum = queryParam.getPage() == null ? 1 : queryParam.getPage();
        long pageSize = queryParam.getPageSize() == null ? 20 : queryParam.getPageSize();
        log.info("分页查询人员管理：pageNum = {}, pageSize = {}, 参数 = {}", pageNum, pageSize, queryParam);

        QueryWrapper<SysUser> queryWrapper = new QueryWrapper<>();
        queryWrapper.like(StringUtils.hasText(queryParam.getName()), "name", queryParam.getName());
        queryWrapper.eq(StringUtils.hasText(queryParam.getEmployeeNo()), "employee_no", queryParam.getEmployeeNo());
        queryWrapper.eq(queryParam.getOrgId() != null, "org_id", queryParam.getOrgId());
        queryWrapper.eq(StringUtils.hasText(queryParam.getStatus()), "status", queryParam.getStatus());
        // 角色不是 sys_user 的列，走 sys_user_role 子查询过滤
        queryWrapper.apply(queryParam.getRoleId() != null,
                "id IN (SELECT user_id FROM sys_user_role WHERE role_id = {0})", queryParam.getRoleId());

        if (StringUtils.hasText(queryParam.getKeyword())) {
            String keyword = queryParam.getKeyword().trim();
            queryWrapper.and(w -> w
                    .like("name", keyword)
                    .or().like("employee_no", keyword)
                    .or().like("phone", keyword));
        }
        queryWrapper.orderByAsc("id");

        Page<SysUser> page = new Page<>(pageNum, pageSize);
        Page<SysUser> resultPage = sysUserMapper.selectPage(page, queryWrapper);
        List<SysUser> records = resultPage.getRecords();
        log.info("分页查询人员管理结果条数：{}", records.size());

        PageInfo<PersonnelVO> pageInfo = new PageInfo<>();
        pageInfo.setPage((int) resultPage.getCurrent());
        pageInfo.setPageSize((int) resultPage.getSize());
        pageInfo.setTotal(resultPage.getTotal());
        pageInfo.setPages((int) ((resultPage.getTotal() + pageSize - 1) / pageSize));
        pageInfo.setList(fillDisplayFields(personnelConvert.entityListToVoList(records)));
        return Result.success(pageInfo);
    }

    @Override
    public Result<PersonnelVO> getById(Long id) {
        log.info("查询人员管理详情，id：{}", id);
        SysUser entity = sysUserMapper.selectById(id);
        if (entity == null) {
            return Result.error("人员不存在，id = " + id);
        }
        PersonnelVO vo = personnelConvert.entityToVo(entity);
        return Result.success(fillDisplayFields(vo));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<Long> add(PersonnelDTO dto) {
        log.info("新增人员管理，参数：{}", dto);
        Result<Long> invalid = validate(dto, null);
        if (invalid != null) {
            return invalid;
        }

        SysUser entity = personnelConvert.dtoToEntity(dto);
        entity.setGender(normalizeGender(entity.getGender()));
        if (!StringUtils.hasText(entity.getStatus())) {
            entity.setStatus(STATUS_ENABLED);
        }
        entity.setCreateBy(currentOperator());
        sysUserMapper.insert(entity);

        saveUserRole(entity.getId(), dto.getRoleId());
        log.info("新增人员管理成功，id = {}", entity.getId());
        return Result.success(entity.getId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<Long> update(Long id, PersonnelDTO dto) {
        log.info("编辑人员管理，id：{}，参数 = {}", id, dto);
        if (sysUserMapper.selectById(id) == null) {
            return Result.error("人员不存在，id = " + id);
        }
        Result<Long> invalid = validate(dto, id);
        if (invalid != null) {
            return invalid;
        }

        SysUser entity = personnelConvert.dtoToEntity(dto);
        entity.setId(id);
        entity.setGender(normalizeGender(entity.getGender()));
        entity.setUpdateBy(currentOperator());
        // updateById 仅更新非空字段：未传字段保留原值
        sysUserMapper.updateById(entity);

        // 角色按「先删后插」整体替换，未传 roleId 时不动原有分配
        if (dto.getRoleId() != null) {
            sysUserRoleMapper.delete(Wrappers.<SysUserRole>lambdaQuery().eq(SysUserRole::getUserId, id));
            saveUserRole(id, dto.getRoleId());
        }
        log.info("编辑人员管理成功，id = {}", id);
        return Result.success(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<Boolean> delete(Long id) {
        log.info("删除人员管理，id：{}", id);
        if (sysUserMapper.selectById(id) == null) {
            return Result.error("人员不存在，id = " + id);
        }
        // 显式清理角色分配（表上虽有 ON DELETE CASCADE，这里不依赖它）
        sysUserRoleMapper.delete(Wrappers.<SysUserRole>lambdaQuery().eq(SysUserRole::getUserId, id));
        int rows = sysUserMapper.deleteById(id);
        log.info("删除人员管理成功，id = {}", id);
        return Result.success(rows > 0);
    }

    /**
     * 新增 / 编辑的公共校验，返回 null 表示通过。
     *
     * @param excludeId 编辑时排除自身（工号唯一性判断用）
     */
    private Result<Long> validate(PersonnelDTO dto, Long excludeId) {
        if (!StringUtils.hasText(dto.getName())) {
            return Result.error("姓名不能为空");
        }
        if (!StringUtils.hasText(dto.getEmployeeNo())) {
            return Result.error("工号不能为空");
        }
        if (!StringUtils.hasText(dto.getPhone())) {
            return Result.error("电话不能为空");
        }
        if (dto.getRoleId() == null) {
            return Result.error("角色不能为空");
        }
        if (dto.getOrgId() == null) {
            return Result.error("所属机构不能为空");
        }
        if (StringUtils.hasText(dto.getGender()) && !isValidGender(dto.getGender())) {
            return Result.error("性别取值不合法，仅支持 male / female（兼容 男 / 女）");
        }
        if (StringUtils.hasText(dto.getStatus())
                && !STATUS_ENABLED.equals(dto.getStatus()) && !STATUS_DISABLED.equals(dto.getStatus())) {
            return Result.error("状态取值不合法，仅支持 enabled / disabled");
        }

        Long dupCount = sysUserMapper.selectCount(Wrappers.<SysUser>lambdaQuery()
                .eq(SysUser::getEmployeeNo, dto.getEmployeeNo())
                .ne(excludeId != null, SysUser::getId, excludeId));
        if (dupCount != null && dupCount > 0) {
            return Result.error("工号已存在：" + dto.getEmployeeNo());
        }
        if (sysRoleMapper.selectById(dto.getRoleId()) == null) {
            return Result.error("角色不存在，roleId = " + dto.getRoleId());
        }
        if (sysOrgMapper.selectById(dto.getOrgId()) == null) {
            return Result.error("所属机构不存在，orgId = " + dto.getOrgId());
        }
        return null;
    }

    /**
     * 写入用户-角色关联（唯一键 user_id + role_id，已存在则跳过）。
     */
    private void saveUserRole(Long userId, Long roleId) {
        if (userId == null || roleId == null) {
            return;
        }
        Long exist = sysUserRoleMapper.selectCount(Wrappers.<SysUserRole>lambdaQuery()
                .eq(SysUserRole::getUserId, userId)
                .eq(SysUserRole::getRoleId, roleId));
        if (exist != null && exist > 0) {
            return;
        }
        SysUserRole userRole = new SysUserRole();
        userRole.setUserId(userId);
        userRole.setRoleId(roleId);
        userRole.setCreateBy(currentOperator());
        sysUserRoleMapper.insert(userRole);
    }

    /**
     * 校验性别取值合法性（历史数据可能是中文「男」「女」）。
     */
    private boolean isValidGender(String value) {
        return GENDER_MALE.equals(value) || GENDER_FEMALE.equals(value)
                || "男".equals(value) || "女".equals(value);
    }

    /**
     * 性别归一为标准 code：男→male，女→female，落库即完成数据修复。
     */
    private String normalizeGender(String gender) {
        if ("男".equals(gender)) {
            return GENDER_MALE;
        }
        if ("女".equals(gender)) {
            return GENDER_FEMALE;
        }
        return gender;
    }

    /**
     * 回填 VO 展示字段：角色名称（sys_user_role + sys_role）、机构名称（sys_org）。
     */
    private List<PersonnelVO> fillDisplayFields(List<PersonnelVO> voList) {
        if (CollectionUtils.isEmpty(voList)) {
            return voList;
        }

        List<Long> userIds = voList.stream()
                .map(PersonnelVO::getId)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
        Map<Long, Long> userToRoleId = userIds.isEmpty()
                ? Collections.emptyMap()
                : sysUserRoleMapper.selectList(Wrappers.<SysUserRole>lambdaQuery()
                        .select(SysUserRole::getUserId, SysUserRole::getRoleId) // 只查询需要字段，避开id
                        .in(SysUserRole::getUserId, userIds)).stream()
                .collect(Collectors.toMap(SysUserRole::getUserId, SysUserRole::getRoleId, (a, b) -> a));

        List<Long> roleIds = userToRoleId.values().stream().distinct().collect(Collectors.toList());
        Map<Long, String> roleIdToName = roleIds.isEmpty()
                ? Collections.emptyMap()
                : sysRoleMapper.selectBatchIds(roleIds).stream()
                .collect(Collectors.toMap(SysRole::getId, SysRole::getRoleName, (a, b) -> a));

        List<Long> orgIds = voList.stream()
                .map(PersonnelVO::getOrgId)
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());
        Map<Long, String> orgIdToName = orgIds.isEmpty()
                ? Collections.emptyMap()
                : sysOrgMapper.selectBatchIds(orgIds).stream()
                .collect(Collectors.toMap(SysOrg::getId, SysOrg::getOrgName, (a, b) -> a));

        voList.forEach(vo -> {
            Long roleId = userToRoleId.get(vo.getId());
            vo.setRoleId(roleId);
            vo.setRoleName(roleId == null ? null : roleIdToName.get(roleId));
            vo.setOrgName(orgIdToName.get(vo.getOrgId()));
        });
        return voList;
    }

    /**
     * 回填单个 VO 的展示字段。
     */
    private PersonnelVO fillDisplayFields(PersonnelVO vo) {
        if (vo == null) {
            return null;
        }
        fillDisplayFields(List.of(vo));
        return vo;
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
