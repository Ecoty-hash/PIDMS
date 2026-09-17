package com.pidms.pidmsbackend.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.pidms.pidmsbackend.common.Result;
import com.pidms.pidmsbackend.common.UserContext;
import com.pidms.pidmsbackend.convert.OrganizationConvert;
import com.pidms.pidmsbackend.dto.OrganizationDTO;
import com.pidms.pidmsbackend.entity.OrganizationQueryParam;
import com.pidms.pidmsbackend.entity.PageInfo;
import com.pidms.pidmsbackend.entity.SysOrg;
import com.pidms.pidmsbackend.entity.SysUser;
import com.pidms.pidmsbackend.mapper.SysOrgMapper;
import com.pidms.pidmsbackend.mapper.SysUserMapper;
import com.pidms.pidmsbackend.service.OrganizationService;
import com.pidms.pidmsbackend.vo.OrganizationVO;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 机构管理 服务实现
 * <p>树形结构：sys_org.parent_id 自关联。列表查询返回平铺数据并回填上级机构名称；
 * 树查询由 Service 在内存中按 parentId 组装，不做递归 SQL。</p>
 */
@Slf4j
@Service
public class OrganizationServiceImpl implements OrganizationService {

    /** 状态：启用 */
    private static final String STATUS_ENABLED = "enabled";
    /** 状态：停用 */
    private static final String STATUS_DISABLED = "disabled";
    /** 防环兜底：向上追溯的最大层数 */
    private static final int MAX_TREE_DEPTH = 100;

    @Resource
    private SysOrgMapper sysOrgMapper;

    @Resource
    private SysUserMapper sysUserMapper;

    @Resource
    private OrganizationConvert organizationConvert;

    @Override
    public Result<PageInfo<OrganizationVO>> pageQuery(OrganizationQueryParam queryParam) {
        long pageNum = queryParam.getPage() == null ? 1 : queryParam.getPage();
        long pageSize = queryParam.getPageSize() == null ? 20 : queryParam.getPageSize();
        log.info("分页查询机构管理：pageNum = {}, pageSize = {}, 参数 = {}", pageNum, pageSize, queryParam);

        QueryWrapper<SysOrg> queryWrapper = new QueryWrapper<>();
        queryWrapper.like(StringUtils.hasText(queryParam.getOrgName()), "org_name", queryParam.getOrgName());
        queryWrapper.eq(StringUtils.hasText(queryParam.getOrgCode()), "org_code", queryParam.getOrgCode());
        queryWrapper.eq(StringUtils.hasText(queryParam.getStatus()), "status", queryParam.getStatus());

        if (StringUtils.hasText(queryParam.getKeyword())) {
            String keyword = queryParam.getKeyword().trim();
            queryWrapper.and(w -> w
                    .like("org_name", keyword)
                    .or().like("org_code", keyword));
        }
        queryWrapper.orderByAsc("sort_order").orderByAsc("id");

        Page<SysOrg> page = new Page<>(pageNum, pageSize);
        Page<SysOrg> resultPage = sysOrgMapper.selectPage(page, queryWrapper);
        List<SysOrg> records = resultPage.getRecords();
        log.info("分页查询机构管理结果条数：{}", records.size());

        PageInfo<OrganizationVO> pageInfo = new PageInfo<>();
        pageInfo.setPage((int) resultPage.getCurrent());
        pageInfo.setPageSize((int) resultPage.getSize());
        pageInfo.setTotal(resultPage.getTotal());
        pageInfo.setPages((int) ((resultPage.getTotal() + pageSize - 1) / pageSize));
        pageInfo.setList(fillParentName(organizationConvert.entityListToVoList(records)));
        return Result.success(pageInfo);
    }

    @Override
    public Result<OrganizationVO> getById(Long id) {
        log.info("查询机构管理详情，id：{}", id);
        SysOrg entity = sysOrgMapper.selectById(id);
        if (entity == null) {
            return Result.error("机构不存在，id = " + id);
        }
        OrganizationVO vo = organizationConvert.entityToVo(entity);
        SysOrg parent = entity.getParentId() == null ? null : sysOrgMapper.selectById(entity.getParentId());
        vo.setParentOrgName(parent == null ? null : parent.getOrgName());
        return Result.success(vo);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<Long> add(OrganizationDTO dto) {
        log.info("新增机构管理，参数：{}", dto);
        Result<Long> invalid = validate(dto, null);
        if (invalid != null) {
            return invalid;
        }

        SysOrg entity = organizationConvert.dtoToEntity(dto);
        if (!StringUtils.hasText(entity.getStatus())) {
            entity.setStatus(STATUS_ENABLED);
        }
        entity.setCreateBy(currentOperator());
        sysOrgMapper.insert(entity);
        log.info("新增机构管理成功，id = {}", entity.getId());
        return Result.success(entity.getId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<Long> update(Long id, OrganizationDTO dto) {
        log.info("编辑机构管理，id：{}，参数：{}", id, dto);
        SysOrg exist = sysOrgMapper.selectById(id);
        if (exist == null) {
            return Result.error("机构不存在，id = " + id);
        }
        Result<Long> invalid = validate(dto, id);
        if (invalid != null) {
            return invalid;
        }
        // 自己不能作为自己的上级，也不能把上级改到自己的子孙节点下（防环）
        if (id.equals(dto.getParentOrgId())) {
            return Result.error("上级机构不能选择自己");
        }
        if (dto.getParentOrgId() != null && isDescendant(dto.getParentOrgId(), id)) {
            return Result.error("上级机构不能选择自己的下级机构");
        }

        SysOrg entity = organizationConvert.dtoToEntity(dto);
        entity.setId(id);
        entity.setUpdateBy(currentOperator());
        // updateById 仅更新非空字段：未传字段保留原值
        sysOrgMapper.updateById(entity);
        log.info("编辑机构管理成功，id = {}", id);
        return Result.success(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<Boolean> delete(Long id) {
        log.info("删除机构管理，id：{}", id);
        if (sysOrgMapper.selectById(id) == null) {
            return Result.error("机构不存在，id = " + id);
        }
        Long childCount = sysOrgMapper.selectCount(Wrappers.<SysOrg>lambdaQuery().eq(SysOrg::getParentId, id));
        if (childCount != null && childCount > 0) {
            return Result.error("该机构下还有 " + childCount + " 个下级机构，请先删除下级机构");
        }
        Long userCount = sysUserMapper.selectCount(Wrappers.<SysUser>lambdaQuery().eq(SysUser::getOrgId, id));
        if (userCount != null && userCount > 0) {
            return Result.error("该机构下还有 " + userCount + " 名人员，请先在人员管理中调整归属");
        }

        int rows = sysOrgMapper.deleteById(id);
        log.info("删除机构管理成功，id = {}", id);
        return Result.success(rows > 0);
    }

    @Override
    public Result<List<OrganizationVO>> tree(String keyword) {
        log.info("查询机构树，keyword：{}", keyword);
        List<SysOrg> all = sysOrgMapper.selectList(Wrappers.<SysOrg>lambdaQuery()
                .orderByAsc(SysOrg::getSortOrder)
                .orderByAsc(SysOrg::getId));
        if (CollectionUtils.isEmpty(all)) {
            return Result.success(new ArrayList<>());
        }

        Map<Long, SysOrg> idToOrg = all.stream()
                .collect(Collectors.toMap(SysOrg::getId, org -> org, (a, b) -> a, LinkedHashMap::new));

        List<SysOrg> visible = all;
        if (StringUtils.hasText(keyword)) {
            // 命中的节点连同其全部上级一起保留，保证树上能看出层级路径
            String kw = keyword.trim();
            Set<Long> keep = new HashSet<>();
            for (SysOrg org : all) {
                if (contains(org.getOrgName(), kw) || contains(org.getOrgCode(), kw)) {
                    keep.add(org.getId());
                    Long cursor = org.getParentId();
                    int depth = 0;
                    while (cursor != null && depth++ < MAX_TREE_DEPTH) {
                        keep.add(cursor);
                        SysOrg parent = idToOrg.get(cursor);
                        cursor = parent == null ? null : parent.getParentId();
                    }
                }
            }
            visible = all.stream().filter(org -> keep.contains(org.getId())).collect(Collectors.toList());
        }

        return Result.success(buildTree(visible));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<OrganizationVO> addChild(Long parentId, OrganizationDTO dto) {
        log.info("新增下级机构，parentId：{}，参数：{}", parentId, dto);
        SysOrg parent = sysOrgMapper.selectById(parentId);
        if (parent == null) {
            return Result.error("上级机构不存在，id = " + parentId);
        }
        // 路径已锁定上级，请求体里的 parentOrgId 以路径为准
        dto.setParentOrgId(parentId);
        dto.setId(null);
        Result<Long> invalid = validate(dto, null);
        if (invalid != null) {
            return Result.error(invalid.getMessage());
        }

        SysOrg entity = organizationConvert.dtoToEntity(dto);
        if (!StringUtils.hasText(entity.getStatus())) {
            entity.setStatus(STATUS_ENABLED);
        }
        entity.setParentId(parentId);
        entity.setCreateBy(currentOperator());
        sysOrgMapper.insert(entity);
        log.info("新增下级机构成功，id = {}，parentId = {}", entity.getId(), parentId);

        OrganizationVO vo = organizationConvert.entityToVo(entity);
        vo.setParentOrgName(parent.getOrgName());
        return Result.success(vo);
    }

    /**
     * 新增 / 编辑的公共校验，返回 null 表示通过。
     *
     * @param excludeId 编辑时排除自身（编码唯一性判断用）
     */
    private Result<Long> validate(OrganizationDTO dto, Long excludeId) {
        if (!StringUtils.hasText(dto.getOrgName())) {
            return Result.error("机构名称不能为空");
        }
        if (!StringUtils.hasText(dto.getOrgCode())) {
            return Result.error("机构编码不能为空");
        }
        if (dto.getSortOrder() == null) {
            return Result.error("机构顺序不能为空");
        }
        if (StringUtils.hasText(dto.getStatus())
                && !STATUS_ENABLED.equals(dto.getStatus()) && !STATUS_DISABLED.equals(dto.getStatus())) {
            return Result.error("状态取值不合法，仅支持 enabled / disabled");
        }

        Long dupCount = sysOrgMapper.selectCount(Wrappers.<SysOrg>lambdaQuery()
                .eq(SysOrg::getOrgCode, dto.getOrgCode())
                .ne(excludeId != null, SysOrg::getId, excludeId));
        if (dupCount != null && dupCount > 0) {
            return Result.error("机构编码已存在：" + dto.getOrgCode());
        }
        if (dto.getParentOrgId() != null && sysOrgMapper.selectById(dto.getParentOrgId()) == null) {
            return Result.error("上级机构不存在，parentOrgId = " + dto.getParentOrgId());
        }
        return null;
    }

    /**
     * 判断 candidateId 是否为 orgId 的子孙节点（用于编辑时防环）。
     */
    private boolean isDescendant(Long candidateId, Long orgId) {
        Long cursor = candidateId;
        int depth = 0;
        while (cursor != null && depth++ < MAX_TREE_DEPTH) {
            if (cursor.equals(orgId)) {
                return true;
            }
            SysOrg node = sysOrgMapper.selectById(cursor);
            cursor = node == null ? null : node.getParentId();
        }
        return false;
    }

    /**
     * 把平铺机构列表组装成树（parentId 不在本次集合内的节点自动提升为根节点）。
     */
    private List<OrganizationVO> buildTree(List<SysOrg> orgs) {
        List<OrganizationVO> voList = organizationConvert.entityListToVoList(orgs);
        Map<Long, OrganizationVO> idToVo = voList.stream()
                .collect(Collectors.toMap(OrganizationVO::getId, vo -> vo, (a, b) -> a, LinkedHashMap::new));
        voList.forEach(vo -> vo.setChildren(new ArrayList<>()));

        List<OrganizationVO> roots = new ArrayList<>();
        for (OrganizationVO vo : voList) {
            OrganizationVO parent = vo.getParentOrgId() == null ? null : idToVo.get(vo.getParentOrgId());
            if (parent == null) {
                roots.add(vo);
            } else {
                parent.getChildren().add(vo);
                vo.setParentOrgName(parent.getOrgName());
            }
        }
        return roots;
    }

    /**
     * 批量回填上级机构名称（列表用）。
     */
    private List<OrganizationVO> fillParentName(List<OrganizationVO> voList) {
        if (CollectionUtils.isEmpty(voList)) {
            return voList;
        }
        List<Long> parentIds = voList.stream()
                .map(OrganizationVO::getParentOrgId)
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());
        Map<Long, String> idToName = parentIds.isEmpty()
                ? Collections.emptyMap()
                : sysOrgMapper.selectBatchIds(parentIds).stream()
                .collect(Collectors.toMap(SysOrg::getId, SysOrg::getOrgName, (a, b) -> a));
        voList.forEach(vo -> vo.setParentOrgName(idToName.get(vo.getParentOrgId())));
        return voList;
    }

    /**
     * 空安全的包含判断。
     */
    private boolean contains(String text, String keyword) {
        return text != null && text.contains(keyword);
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
