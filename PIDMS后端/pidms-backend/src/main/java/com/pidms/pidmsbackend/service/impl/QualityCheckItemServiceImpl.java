package com.pidms.pidmsbackend.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.pidms.pidmsbackend.common.Result;
import com.pidms.pidmsbackend.common.UserContext;
import com.pidms.pidmsbackend.convert.QualityCheckItemConvert;
import com.pidms.pidmsbackend.dto.QualityCheckItemDTO;
import com.pidms.pidmsbackend.entity.PageInfo;
import com.pidms.pidmsbackend.entity.QualityCheckItem;
import com.pidms.pidmsbackend.entity.QualityCheckItemQueryParam;
import com.pidms.pidmsbackend.mapper.QualityCheckItemMapper;
import com.pidms.pidmsbackend.service.QualityCheckItemService;
import com.pidms.pidmsbackend.vo.QualityCheckItemVO;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;

/**
 * 质量检查项 服务实现
 * <p>状态取值：enabled启用 / disabled停用 / sealed封存；封存走专用动作接口。</p>
 */
@Slf4j
@Service
public class QualityCheckItemServiceImpl implements QualityCheckItemService {

    /** 状态：启用 */
    private static final String STATUS_ENABLED = "enabled";
    /** 状态：停用 */
    private static final String STATUS_DISABLED = "disabled";
    /** 状态：封存 */
    private static final String STATUS_SEALED = "sealed";

    @Resource
    private QualityCheckItemMapper qualityCheckItemMapper;
    @Resource
    private QualityCheckItemConvert qualityCheckItemConvert;

    @Override
    public Result<PageInfo<QualityCheckItemVO>> pageQuery(QualityCheckItemQueryParam queryParam) {
        long pageNum = queryParam.getPage() == null ? 1 : queryParam.getPage();
        long pageSize = queryParam.getPageSize() == null ? 20 : queryParam.getPageSize();
        log.info("分页查询质量检查项：pageNum = {}, pageSize = {}, 参数 = {}", pageNum, pageSize, queryParam);

        QueryWrapper<QualityCheckItem> queryWrapper = new QueryWrapper<>();
        // 高级搜索：所属分类
        queryWrapper.eq(StringUtils.hasText(queryParam.getCategory()), "category", queryParam.getCategory());
        // 高级搜索：状态
        queryWrapper.eq(StringUtils.hasText(queryParam.getStatus()), "status", queryParam.getStatus());
        // 关键字：检查项名称
        if (StringUtils.hasText(queryParam.getKeyword())) {
            queryWrapper.like("check_item_name", queryParam.getKeyword().trim());
        }
        queryWrapper.orderByAsc("id");

        Page<QualityCheckItem> page = new Page<>(pageNum, pageSize);
        Page<QualityCheckItem> resultPage = qualityCheckItemMapper.selectPage(page, queryWrapper);
        List<QualityCheckItem> records = resultPage.getRecords();
        log.info("分页查询质量检查项结果条数：{}", records.size());

        PageInfo<QualityCheckItemVO> pageInfo = new PageInfo<>();
        pageInfo.setPage((int) resultPage.getCurrent());
        pageInfo.setPageSize((int) resultPage.getSize());
        pageInfo.setTotal(resultPage.getTotal());
        pageInfo.setPages((int) ((resultPage.getTotal() + pageSize - 1) / pageSize));
        pageInfo.setList(qualityCheckItemConvert.entityListToVoList(records));
        return Result.success(pageInfo);
    }

    @Override
    public Result<QualityCheckItemVO> getById(Long id) {
        QualityCheckItem entity = qualityCheckItemMapper.selectById(id);
        if (entity == null) {
            return Result.error("质量检查项不存在，id = " + id);
        }
        return Result.success(qualityCheckItemConvert.entityToVo(entity));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<Long> add(QualityCheckItemDTO dto) {
        if (!StringUtils.hasText(dto.getCheckItemName())) {
            return Result.error("检查项名称不能为空");
        }
        QualityCheckItem entity = qualityCheckItemConvert.dtoToEntity(dto);
        if (!StringUtils.hasText(entity.getStatus())) {
            entity.setStatus(STATUS_ENABLED);
        }
        // 新增只允许 enabled/disabled（封存必须走封存动作接口）
        if (!STATUS_ENABLED.equals(entity.getStatus()) && !STATUS_DISABLED.equals(entity.getStatus())) {
            return Result.error("状态取值不合法，仅支持 enabled/disabled");
        }
        entity.setCreateBy(currentOperator());
        qualityCheckItemMapper.insert(entity);
        log.info("新增质量检查项成功，id = {}", entity.getId());
        return Result.success(entity.getId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<Long> update(Long id, QualityCheckItemDTO dto) {
        QualityCheckItem exist = qualityCheckItemMapper.selectById(id);
        if (exist == null) {
            return Result.error("质量检查项不存在，id = " + id);
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

        QualityCheckItem entity = qualityCheckItemConvert.dtoToEntity(dto);
        entity.setId(id);
        entity.setUpdateBy(currentOperator());
        // updateById 仅更新非空字段：未传字段保留原值
        qualityCheckItemMapper.updateById(entity);
        log.info("编辑质量检查项成功，id = {}", id);
        return Result.success(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<Boolean> delete(Long id) {
        if (qualityCheckItemMapper.selectById(id) == null) {
            return Result.error("质量检查项不存在，id = " + id);
        }
        int rows = qualityCheckItemMapper.deleteById(id);
        log.info("删除质量检查项成功，id = {}", id);
        return Result.success(rows > 0);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<Boolean> seal(Long id) {
        QualityCheckItem exist = qualityCheckItemMapper.selectById(id);
        if (exist == null) {
            return Result.error("质量检查项不存在，id = " + id);
        }
        if (STATUS_SEALED.equals(exist.getStatus())) {
            return Result.error("该质量检查项已封存，无需重复操作");
        }
        qualityCheckItemMapper.update(null, Wrappers.<QualityCheckItem>lambdaUpdate()
                .eq(QualityCheckItem::getId, id)
                .set(QualityCheckItem::getStatus, STATUS_SEALED)
                .set(QualityCheckItem::getUpdateBy, currentOperator()));
        log.info("封存质量检查项成功，id = {}", id);
        return Result.success(true);
    }

    private String currentOperator() {
        if (UserContext.getUser() == null) {
            return null;
        }
        return UserContext.getUser().getUsername();
    }
}
