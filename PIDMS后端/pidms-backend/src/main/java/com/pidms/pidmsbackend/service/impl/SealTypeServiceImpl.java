package com.pidms.pidmsbackend.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.pidms.pidmsbackend.common.Result;
import com.pidms.pidmsbackend.common.UserContext;
import com.pidms.pidmsbackend.convert.SealTypeConvert;
import com.pidms.pidmsbackend.dto.SealTypeDTO;
import com.pidms.pidmsbackend.entity.PageInfo;
import com.pidms.pidmsbackend.entity.SealApplication;
import com.pidms.pidmsbackend.entity.SealType;
import com.pidms.pidmsbackend.entity.SealTypeQueryParam;
import com.pidms.pidmsbackend.mapper.SealApplicationMapper;
import com.pidms.pidmsbackend.mapper.SealTypeMapper;
import com.pidms.pidmsbackend.service.SealTypeService;
import com.pidms.pidmsbackend.vo.SealTypeVO;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;

/**
 * 印章类型字典服务
 * <p>状态取值：enabled启用 / disabled停用 / sealed封存；封存/解封走专用动作接口。</p>
 */
@Slf4j
@Service
public class SealTypeServiceImpl implements SealTypeService {

    /** 字典状态：启用 */
    private static final String STATUS_ENABLED = "enabled";
    /** 字典状态：停用 */
    private static final String STATUS_DISABLED = "disabled";
    /** 字典状态：封存 */
    private static final String STATUS_SEALED = "sealed";

    @Resource
    private SealTypeMapper sealTypeMapper;
    @Resource
    private SealApplicationMapper sealApplicationMapper;
    @Resource
    private SealTypeConvert sealTypeConvert;

    @Override
    public Result<PageInfo<SealTypeVO>> pageQuery(SealTypeQueryParam queryParam) {
        long pageNum = queryParam.getPage() == null ? 1 : queryParam.getPage();
        long pageSize = queryParam.getPageSize() == null ? 20 : queryParam.getPageSize();
        log.info("分页查询印章类型：pageNum = {}, pageSize = {}, 参数 = {}", pageNum, pageSize, queryParam);

        QueryWrapper<SealType> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq(StringUtils.hasText(queryParam.getStatus()), "status", queryParam.getStatus());
        if (StringUtils.hasText(queryParam.getKeyword())) {
            queryWrapper.like("type_name", queryParam.getKeyword());
        }
        queryWrapper.orderByAsc("id");

        Page<SealType> page = new Page<>(pageNum, pageSize);
        Page<SealType> resultPage = sealTypeMapper.selectPage(page, queryWrapper);
        List<SealType> records = resultPage.getRecords();
        log.info("分页查询印章类型结果条数：{}", records.size());

        PageInfo<SealTypeVO> pageInfo = new PageInfo<>();
        pageInfo.setPage((int) pageNum);
        pageInfo.setPageSize((int) pageSize);
        pageInfo.setTotal(resultPage.getTotal());
        pageInfo.setPages((int) ((resultPage.getTotal() + pageSize - 1) / pageSize));
        pageInfo.setList(sealTypeConvert.entityListToVoList(records));
        return Result.success(pageInfo);
    }

    @Override
    public Result<SealTypeVO> getById(Long id) {
        SealType entity = sealTypeMapper.selectById(id);
        if (entity == null) {
            return Result.error("印章类型不存在，id = " + id);
        }
        return Result.success(sealTypeConvert.entityToVo(entity));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<Long> add(SealTypeDTO dto) {
        if (!StringUtils.hasText(dto.getTypeName())) {
            return Result.error("印章类型名称不能为空");
        }
        Long dupCount = sealTypeMapper.selectCount(Wrappers.<SealType>lambdaQuery()
                .eq(SealType::getTypeName, dto.getTypeName()));
        if (dupCount != null && dupCount > 0) {
            return Result.error("印章类型名称已存在：" + dto.getTypeName());
        }

        SealType entity = sealTypeConvert.dtoToEntity(dto);
        if (!StringUtils.hasText(entity.getStatus())) {
            entity.setStatus(STATUS_ENABLED);
        }
        // 兼容旧数据 legacy 状态码（active/inactive），保存时归一为标准码
        entity.setStatus(normalizeStatus(entity.getStatus()));
        if (!STATUS_ENABLED.equals(entity.getStatus()) && !STATUS_DISABLED.equals(entity.getStatus())) {
            return Result.error("状态取值不合法，仅支持 enabled/disabled");
        }
        entity.setCreateBy(currentOperator());
        sealTypeMapper.insert(entity);
        log.info("新增印章类型成功，id = {}", entity.getId());
        return Result.success(entity.getId());
    }

    /**
     * 旧数据 legacy 状态码归一为标准码（active→enabled / inactive→disabled）。
     * 保证历史遗留行在「编辑保存」时不会因状态非法而 500，同时落库即完成数据修复。
     */
    private String normalizeStatus(String status) {
        if (STATUS_ENABLED.equals(status) || STATUS_DISABLED.equals(status) || STATUS_SEALED.equals(status)) {
            return status;
        }
        if ("active".equals(status)) {
            return STATUS_ENABLED;
        }
        if ("inactive".equals(status)) {
            return STATUS_DISABLED;
        }
        return status;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<Long> update(Long id, SealTypeDTO dto) {
        SealType exist = sealTypeMapper.selectById(id);
        if (exist == null) {
            return Result.error("印章类型不存在，id = " + id);
        }
        if (StringUtils.hasText(dto.getTypeName())) {
            Long dupCount = sealTypeMapper.selectCount(Wrappers.<SealType>lambdaQuery()
                    .eq(SealType::getTypeName, dto.getTypeName())
                    .ne(SealType::getId, id));
            if (dupCount != null && dupCount > 0) {
                return Result.error("印章类型名称已存在：" + dto.getTypeName());
            }
        }
        // 兼容旧数据 legacy 状态码（active/inactive），保存时归一为标准码
        if (StringUtils.hasText(dto.getStatus())) {
            dto.setStatus(normalizeStatus(dto.getStatus()));
        }
        if (StringUtils.hasText(dto.getStatus())
                && !STATUS_ENABLED.equals(dto.getStatus())
                && !STATUS_DISABLED.equals(dto.getStatus())
                && !STATUS_SEALED.equals(dto.getStatus())) {
            return Result.error("状态取值不合法，仅支持 enabled/disabled/sealed");
        }

        SealType entity = sealTypeConvert.dtoToEntity(dto);
        entity.setId(id);
        entity.setUpdateBy(currentOperator());
        sealTypeMapper.updateById(entity);
        log.info("编辑印章类型成功，id = {}", id);
        return Result.success(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<Boolean> delete(Long id) {
        SealType exist = sealTypeMapper.selectById(id);
        if (exist == null) {
            return Result.error("印章类型不存在，id = " + id);
        }
        // 已被用印申请引用时不允许删除（友好提示，数据库外键同样兜底）
        Long refCount = sealApplicationMapper.selectCount(Wrappers.<SealApplication>lambdaQuery()
                .eq(SealApplication::getSealTypeId, id));
        if (refCount != null && refCount > 0) {
            return Result.error("该印章类型已被用印申请引用，无法删除");
        }
        int rows = sealTypeMapper.deleteById(id);
        log.info("删除印章类型成功，id = {}", id);
        return Result.success(rows > 0);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<Boolean> seal(Long id) {
        SealType exist = sealTypeMapper.selectById(id);
        if (exist == null) {
            return Result.error("印章类型不存在，id = " + id);
        }
        if (STATUS_SEALED.equals(exist.getStatus())) {
            return Result.error("该印章类型已封存，无需重复操作");
        }
        sealTypeMapper.update(null, Wrappers.<SealType>lambdaUpdate()
                .eq(SealType::getId, id)
                .set(SealType::getStatus, STATUS_SEALED)
                .set(SealType::getUpdateBy, currentOperator()));
        log.info("封存印章类型成功，id = {}", id);
        return Result.success(true);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<Boolean> unseal(Long id) {
        SealType exist = sealTypeMapper.selectById(id);
        if (exist == null) {
            return Result.error("印章类型不存在，id = " + id);
        }
        if (!STATUS_SEALED.equals(exist.getStatus())) {
            return Result.error("该印章类型未封存，无需解封");
        }
        sealTypeMapper.update(null, Wrappers.<SealType>lambdaUpdate()
                .eq(SealType::getId, id)
                .set(SealType::getStatus, STATUS_ENABLED)
                .set(SealType::getUpdateBy, currentOperator()));
        log.info("解封印章类型成功，id = {}", id);
        return Result.success(true);
    }

    private String currentOperator() {
        if (UserContext.getUser() == null) {
            return null;
        }
        return UserContext.getUser().getUsername();
    }
}
