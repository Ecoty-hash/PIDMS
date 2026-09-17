package com.pidms.pidmsbackend.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.pidms.pidmsbackend.common.Result;
import com.pidms.pidmsbackend.common.UserContext;
import com.pidms.pidmsbackend.convert.RoleConvert;
import com.pidms.pidmsbackend.dto.RoleDTO;
import com.pidms.pidmsbackend.entity.PageInfo;
import com.pidms.pidmsbackend.entity.RoleQueryParam;
import com.pidms.pidmsbackend.entity.SysRole;
import com.pidms.pidmsbackend.entity.SysUserRole;
import com.pidms.pidmsbackend.mapper.SysRoleMapper;
import com.pidms.pidmsbackend.mapper.SysUserRoleMapper;
import com.pidms.pidmsbackend.service.RoleService;
import com.pidms.pidmsbackend.vo.RoleVO;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * 角色管理 服务实现
 * <p>一期只维护 sys_role 本体。DTO 中的 permissionSettings / userAssignments 接收但不处理，
 * VO 中对应字段返回空值，权限与用户分配留待二期。</p>
 */
@Slf4j
@Service
public class RoleServiceImpl implements RoleService {

    /** 状态：启用 */
    private static final String STATUS_ENABLED = "enabled";
    /** 状态：禁用 */
    private static final String STATUS_DISABLED = "disabled";

    @Resource
    private SysRoleMapper sysRoleMapper;

    @Resource
    private SysUserRoleMapper sysUserRoleMapper;

    @Resource
    private RoleConvert roleConvert;

    @Override
    public Result<PageInfo<RoleVO>> pageQuery(RoleQueryParam queryParam) {
        long pageNum = queryParam.getPage() == null ? 1 : queryParam.getPage();
        long pageSize = queryParam.getPageSize() == null ? 20 : queryParam.getPageSize();
        log.info("分页查询角色管理：pageNum = {}, pageSize = {}, 参数 = {}", pageNum, pageSize, queryParam);

        QueryWrapper<SysRole> queryWrapper = new QueryWrapper<>();
        queryWrapper.like(StringUtils.hasText(queryParam.getRoleName()), "role_name", queryParam.getRoleName());
        queryWrapper.eq(StringUtils.hasText(queryParam.getRoleCode()), "role_code", queryParam.getRoleCode());
        queryWrapper.eq(StringUtils.hasText(queryParam.getStatus()), "status", queryParam.getStatus());

        if (StringUtils.hasText(queryParam.getKeyword())) {
            String keyword = queryParam.getKeyword().trim();
            queryWrapper.and(w -> w
                    .like("role_name", keyword)
                    .or().like("role_code", keyword));
        }
        queryWrapper.orderByAsc("id");

        Page<SysRole> page = new Page<>(pageNum, pageSize);
        Page<SysRole> resultPage = sysRoleMapper.selectPage(page, queryWrapper);
        List<SysRole> records = resultPage.getRecords();
        log.info("分页查询角色管理结果条数：{}", records.size());

        PageInfo<RoleVO> pageInfo = new PageInfo<>();
        pageInfo.setPage((int) resultPage.getCurrent());
        pageInfo.setPageSize((int) resultPage.getSize());
        pageInfo.setTotal(resultPage.getTotal());
        pageInfo.setPages((int) ((resultPage.getTotal() + pageSize - 1) / pageSize));
        pageInfo.setList(fillReservedFields(roleConvert.entityListToVoList(records)));
        return Result.success(pageInfo);
    }

    @Override
    public Result<RoleVO> getById(Long id) {
        log.info("查询角色管理详情，id：{}", id);
        SysRole entity = sysRoleMapper.selectById(id);
        if (entity == null) {
            return Result.error("角色不存在，id = " + id);
        }
        return Result.success(fillReservedFields(roleConvert.entityToVo(entity)));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<Long> add(RoleDTO dto) {
        log.info("新增角色管理，参数：{}", dto);
        Result<Long> invalid = validate(dto, null);
        if (invalid != null) {
            return invalid;
        }

        SysRole entity = roleConvert.dtoToEntity(dto);
        if (!StringUtils.hasText(entity.getStatus())) {
            entity.setStatus(STATUS_ENABLED);
        }
        entity.setCreateBy(currentOperator());
        sysRoleMapper.insert(entity);
        log.info("新增角色管理成功，id = {}", entity.getId());
        return Result.success(entity.getId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<Long> update(Long id, RoleDTO dto) {
        log.info("编辑角色管理，id：{}，参数：{}", id, dto);
        if (sysRoleMapper.selectById(id) == null) {
            return Result.error("角色不存在，id = " + id);
        }
        Result<Long> invalid = validate(dto, id);
        if (invalid != null) {
            return invalid;
        }

        SysRole entity = roleConvert.dtoToEntity(dto);
        entity.setId(id);
        entity.setUpdateBy(currentOperator());
        // updateById 仅更新非空字段：未传字段保留原值
        sysRoleMapper.updateById(entity);
        log.info("编辑角色管理成功，id = {}", id);
        return Result.success(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<Boolean> delete(Long id) {
        log.info("删除角色管理，id：{}", id);
        if (sysRoleMapper.selectById(id) == null) {
            return Result.error("角色不存在，id = " + id);
        }
        // sys_user_role 上有 ON DELETE CASCADE，这里先拦住，避免静默解绑用户
        Long userCount = sysUserRoleMapper.selectCount(Wrappers.<SysUserRole>lambdaQuery()
                .eq(SysUserRole::getRoleId, id));
        if (userCount != null && userCount > 0) {
            return Result.error("该角色已分配给 " + userCount + " 名人员，请先解除分配");
        }

        int rows = sysRoleMapper.deleteById(id);
        log.info("删除角色管理成功，id = {}", id);
        return Result.success(rows > 0);
    }

    /**
     * 新增 / 编辑的公共校验，返回 null 表示通过。
     *
     * @param excludeId 编辑时排除自身（编码唯一性判断用）
     */
    private Result<Long> validate(RoleDTO dto, Long excludeId) {
        if (!StringUtils.hasText(dto.getRoleName())) {
            return Result.error("角色名称不能为空");
        }
        if (!StringUtils.hasText(dto.getRoleCode())) {
            return Result.error("角色编码不能为空");
        }
        if (StringUtils.hasText(dto.getStatus())
                && !STATUS_ENABLED.equals(dto.getStatus()) && !STATUS_DISABLED.equals(dto.getStatus())) {
            return Result.error("状态取值不合法，仅支持 enabled / disabled");
        }
        Long dupCount = sysRoleMapper.selectCount(Wrappers.<SysRole>lambdaQuery()
                .eq(SysRole::getRoleCode, dto.getRoleCode())
                .ne(excludeId != null, SysRole::getId, excludeId));
        if (dupCount != null && dupCount > 0) {
            return Result.error("角色编码已存在：" + dto.getRoleCode());
        }
        return null;
    }

    /**
     * 权限相关字段为二期预留：统一回填空数组，避免前端拿到 null 渲染异常。
     */
    private RoleVO fillReservedFields(RoleVO vo) {
        if (vo != null) {
            vo.setPermissionSettings(new ArrayList<>());
        }
        return vo;
    }

    /**
     * 权限相关字段为二期预留：统一回填空数组，避免前端拿到 null 渲染异常。
     */
    private List<RoleVO> fillReservedFields(List<RoleVO> voList) {
        if (voList != null) {
            voList.forEach(vo -> vo.setPermissionSettings(new ArrayList<>()));
        }
        return voList;
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
