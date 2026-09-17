package com.pidms.pidmsbackend.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.pidms.pidmsbackend.common.Result;
import com.pidms.pidmsbackend.common.UserContext;
import com.pidms.pidmsbackend.convert.SafetyCheckItemConvert;
import com.pidms.pidmsbackend.dto.SafetyCheckItemDTO;
import com.pidms.pidmsbackend.entity.PageInfo;
import com.pidms.pidmsbackend.entity.SafetyCheckItem;
import com.pidms.pidmsbackend.entity.SafetyCheckItemQueryParam;
import com.pidms.pidmsbackend.mapper.SafetyCheckItemMapper;
import com.pidms.pidmsbackend.service.SafetyCheckItemService;
import com.pidms.pidmsbackend.vo.SafetyCheckItemVO;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;

/**
 * 安全检查项 服务实现
 * <p>状态取值：enabled启用 / disabled停用 / sealed封存；封存走专用动作接口。</p>
 */
@Slf4j
@Service
public class SafetyCheckItemServiceImpl implements SafetyCheckItemService {

    /** 状态：启用 */
    private static final String STATUS_ENABLED = "enabled";
    /** 状态：停用 */
    private static final String STATUS_DISABLED = "disabled";
    /** 状态：封存 */
    private static final String STATUS_SEALED = "sealed";

    @Resource
    private SafetyCheckItemMapper safetyCheckItemMapper;
    @Resource
    private SafetyCheckItemConvert safetyCheckItemConvert;

    @Override
    public Result<PageInfo<SafetyCheckItemVO>> pageQuery(SafetyCheckItemQueryParam queryParam) {
        long pageNum = queryParam.getPage() == null ? 1 : queryParam.getPage();
        long pageSize = queryParam.getPageSize() == null ? 20 : queryParam.getPageSize();
        log.info("分页查询安全检查项：pageNum = {}, pageSize = {}, 参数 = {}", pageNum, pageSize, queryParam);

        QueryWrapper<SafetyCheckItem> queryWrapper = new QueryWrapper<>();
        // 高级搜索：所属分类
        queryWrapper.eq(StringUtils.hasText(queryParam.getCategory()), "category", queryParam.getCategory());
        // 高级搜索：状态
        queryWrapper.eq(StringUtils.hasText(queryParam.getStatus()), "status", queryParam.getStatus());
        // 关键字：检查项名称
        if (StringUtils.hasText(queryParam.getKeyword())) {
            queryWrapper.like("check_item_name", queryParam.getKeyword().trim());
        }
        queryWrapper.orderByAsc("id");

        Page<SafetyCheckItem> page = new Page<>(pageNum, pageSize);
        Page<SafetyCheckItem> resultPage = safetyCheckItemMapper.selectPage(page, queryWrapper);
        List<SafetyCheckItem> records = resultPage.getRecords();
        log.info("分页查询安全检查项结果条数：{}", records.size());

        PageInfo<SafetyCheckItemVO> pageInfo = new PageInfo<>();
        pageInfo.setPage((int) resultPage.getCurrent());
        pageInfo.setPageSize((int) resultPage.getSize());
        pageInfo.setTotal(resultPage.getTotal());
        pageInfo.setPages((int) ((resultPage.getTotal() + pageSize - 1) / pageSize));
        pageInfo.setList(safetyCheckItemConvert.entityListToVoList(records));
        return Result.success(pageInfo);
    }

    @Override
    public Result<SafetyCheckItemVO> getById(Long id) {
        SafetyCheckItem entity = safetyCheckItemMapper.selectById(id);
        if (entity == null) {
            return Result.error("安全检查项不存在，id = " + id);
        }
        return Result.success(safetyCheckItemConvert.entityToVo(entity));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<Long> add(SafetyCheckItemDTO dto) {
        if (!StringUtils.hasText(dto.getCheckItemName())) {
            return Result.error("检查项名称不能为空");
        }
        SafetyCheckItem entity = safetyCheckItemConvert.dtoToEntity(dto);
        if (!StringUtils.hasText(entity.getStatus())) {
            entity.setStatus(STATUS_ENABLED);
        }
        // 新增只允许 enabled/disabled（封存必须走封存动作接口）
        if (!STATUS_ENABLED.equals(entity.getStatus()) && !STATUS_DISABLED.equals(entity.getStatus())) {
            return Result.error("状态取值不合法，仅支持 enabled/disabled");
        }
        entity.setCreateBy(currentOperator());
        safetyCheckItemMapper.insert(entity);
        log.info("新增安全检查项成功，id = {}", entity.getId());
        return Result.success(entity.getId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<Long> update(Long id, SafetyCheckItemDTO dto) {
        SafetyCheckItem exist = safetyCheckItemMapper.selectById(id);
        if (exist == null) {
            return Result.error("安全检查项不存在，id = " + id);
        }
        if (dto.getCheckItemName() != null && !StringUtils.hasText(dto.getCheckItemName())) {
            return Result.error("检查项名称不能为空");
        }
        if (StringUtils.hasText(dto.getStatus())
                && !STATUS_ENABLED.equals(dto.getStatus())
                && !STATUS_DISABLED.equals(dto.getStatus())
                && !STATUS_SEALED.equals(dto.getStatus())) {
            return Result.error("状态取值不合法，仅支持 enabled/disabled/sealed");
        }

        SafetyCheckItem entity = safetyCheckItemConvert.dtoToEntity(dto);
        entity.setId(id);
        entity.setUpdateBy(currentOperator());
        // updateById 仅更新非空字段：未传字段保留原值
        safetyCheckItemMapper.updateById(entity);
        log.info("编辑安全检查项成功，id = {}", id);
        return Result.success(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<Boolean> delete(Long id) {
        if (safetyCheckItemMapper.selectById(id) == null) {
            return Result.error("安全检查项不存在，id = " + id);
        }
        int rows = safetyCheckItemMapper.deleteById(id);
        log.info("删除安全检查项成功，id = {}", id);
        return Result.success(rows > 0);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<Boolean> seal(Long id) {
        SafetyCheckItem exist = safetyCheckItemMapper.selectById(id);
        if (exist == null) {
            return Result.error("安全检查项不存在，id = " + id);
        }
        if (STATUS_SEALED.equals(exist.getStatus())) {
            return Result.error("该安全检查项已封存，无需重复操作");
        }
        safetyCheckItemMapper.update(null, Wrappers.<SafetyCheckItem>lambdaUpdate()
                .eq(SafetyCheckItem::getId, id)
                .set(SafetyCheckItem::getStatus, STATUS_SEALED)
                .set(SafetyCheckItem::getUpdateBy, currentOperator()));
        log.info("封存安全检查项成功，id = {}", id);
        return Result.success(true);
    }

    private String currentOperator() {
        if (UserContext.getUser() == null) {
            return null;
        }
        return UserContext.getUser().getUsername();
    }
}
