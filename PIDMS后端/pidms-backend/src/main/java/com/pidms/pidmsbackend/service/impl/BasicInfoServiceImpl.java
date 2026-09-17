package com.pidms.pidmsbackend.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.pidms.pidmsbackend.common.Result;
import com.pidms.pidmsbackend.common.UserContext;
import com.pidms.pidmsbackend.convert.BasicInfoConvert;
import com.pidms.pidmsbackend.dto.BasicInfoDTO;
import com.pidms.pidmsbackend.entity.BasicInfo;
import com.pidms.pidmsbackend.entity.BasicInfoQueryParam;
import com.pidms.pidmsbackend.entity.PageInfo;
import com.pidms.pidmsbackend.mapper.BasicInfoMapper;
import com.pidms.pidmsbackend.service.BasicInfoService;
import com.pidms.pidmsbackend.vo.BasicInfoVO;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;

/**
 * 基础资料 服务实现
 */
@Slf4j
@Service
public class BasicInfoServiceImpl implements BasicInfoService {

    /** infoCategory：material */
    private static final String INFO_CATEGORY_MATERIAL = "material";
    /** infoCategory：equipment */
    private static final String INFO_CATEGORY_EQUIPMENT = "equipment";
    /** infoCategory：worktype */
    private static final String INFO_CATEGORY_WORKTYPE = "worktype";

    /** status：active */
    private static final String STATUS_ACTIVE = "active";
    /** status：archived */
    private static final String STATUS_ARCHIVED = "archived";

    @Resource
    private BasicInfoMapper basicInfoMapper;

    @Resource
    private BasicInfoConvert basicInfoConvert;

    @Override
    public Result<PageInfo<BasicInfoVO>> pageQuery(BasicInfoQueryParam queryParam) {
        long pageNum = queryParam.getPage() == null ? 1 : queryParam.getPage();
        long pageSize = queryParam.getPageSize() == null ? 20 : queryParam.getPageSize();
        log.info("分页查询基础资料：pageNum = {}, pageSize = {}, 参数 = {}", pageNum, pageSize, queryParam);

        QueryWrapper<BasicInfo> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq(StringUtils.hasText(queryParam.getInfoCategory()), "info_category", queryParam.getInfoCategory());
        queryWrapper.eq(StringUtils.hasText(queryParam.getStatus()), "status", queryParam.getStatus());

        if (StringUtils.hasText(queryParam.getKeyword())) {
            String keyword = queryParam.getKeyword().trim();
            queryWrapper.and(w -> w
                        .like("info_code", keyword)
                        .or().like("info_name", keyword));
        }
        queryWrapper.orderByAsc("id");

        Page<BasicInfo> page = new Page<>(pageNum, pageSize);
        Page<BasicInfo> resultPage = basicInfoMapper.selectPage(page, queryWrapper);
        List<BasicInfo> records = resultPage.getRecords();
        log.info("分页查询基础资料结果条数：{}", records.size());

        PageInfo<BasicInfoVO> pageInfo = new PageInfo<>();
        pageInfo.setPage((int) resultPage.getCurrent());
        pageInfo.setPageSize((int) resultPage.getSize());
        pageInfo.setTotal(resultPage.getTotal());
        pageInfo.setPages((int) ((resultPage.getTotal() + pageSize - 1) / pageSize));
        pageInfo.setList(basicInfoConvert.entityListToVoList(records));
        return Result.success(pageInfo);
    }

    @Override
    public Result<BasicInfoVO> getById(Long id) {
        log.info("查询基础资料详情，id：{}", id);
        BasicInfo entity = basicInfoMapper.selectById(id);
        if (entity == null) {
            return Result.error("基础资料不存在，id = " + id);
        }
        return Result.success(basicInfoConvert.entityToVo(entity));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<Long> add(BasicInfoDTO dto) {
        log.info("新增基础资料，参数：{}", dto);
        if (!StringUtils.hasText(dto.getInfoCode())) {
            return Result.error("资料编号不能为空");
        }
        if (!StringUtils.hasText(dto.getInfoName())) {
            return Result.error("资料名称不能为空");
        }
        if (!StringUtils.hasText(dto.getInfoCategory())) {
            return Result.error("资料分类不能为空");
        }
        if (StringUtils.hasText(dto.getInfoCategory()) && !isValidInfoCategory(dto.getInfoCategory())) {
            return Result.error("资料分类取值不合法，仅支持 material / equipment / worktype");
        }
        if (StringUtils.hasText(dto.getStatus()) && !isValidStatus(dto.getStatus())) {
            return Result.error("状态取值不合法，仅支持 active / archived");
        }
        if (StringUtils.hasText(dto.getInfoCode())) {
            Long dupCount = basicInfoMapper.selectCount(Wrappers.<BasicInfo>lambdaQuery()
                    .eq(BasicInfo::getInfoCode, dto.getInfoCode()));
            if (dupCount != null && dupCount > 0) {
                return Result.error("资料编号已存在：" + dto.getInfoCode());
            }
        }
        BasicInfo entity = basicInfoConvert.dtoToEntity(dto);
        if (!StringUtils.hasText(entity.getStatus())) {
            entity.setStatus("active");
        }
        entity.setCreateBy(currentOperator());
        basicInfoMapper.insert(entity);
        log.info("新增基础资料成功，id = {}", entity.getId());
        return Result.success(entity.getId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<Long> update(Long id, BasicInfoDTO dto) {
        log.info("编辑基础资料，id：{}，参数：{}", id, dto);
        BasicInfo exist = basicInfoMapper.selectById(id);
        if (exist == null) {
            return Result.error("基础资料不存在，id = " + id);
        }
        if (dto.getInfoCode() != null && !StringUtils.hasText(dto.getInfoCode())) {
            return Result.error("资料编号不能为空");
        }
        if (dto.getInfoName() != null && !StringUtils.hasText(dto.getInfoName())) {
            return Result.error("资料名称不能为空");
        }
        if (dto.getInfoCategory() != null && !StringUtils.hasText(dto.getInfoCategory())) {
            return Result.error("资料分类不能为空");
        }
        if (StringUtils.hasText(dto.getInfoCategory()) && !isValidInfoCategory(dto.getInfoCategory())) {
            return Result.error("资料分类取值不合法，仅支持 material / equipment / worktype");
        }
        if (StringUtils.hasText(dto.getStatus()) && !isValidStatus(dto.getStatus())) {
            return Result.error("状态取值不合法，仅支持 active / archived");
        }
        if (StringUtils.hasText(dto.getInfoCode())) {
            Long dupCount = basicInfoMapper.selectCount(Wrappers.<BasicInfo>lambdaQuery()
                    .eq(BasicInfo::getInfoCode, dto.getInfoCode())
                    .ne(BasicInfo::getId, id));
            if (dupCount != null && dupCount > 0) {
                return Result.error("资料编号已存在：" + dto.getInfoCode());
            }
        }

        BasicInfo entity = basicInfoConvert.dtoToEntity(dto);
        entity.setId(id);
        entity.setUpdateBy(currentOperator());
        // updateById 仅更新非空字段：未传字段保留原值
        basicInfoMapper.updateById(entity);
        log.info("编辑基础资料成功，id = {}", id);
        return Result.success(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<Boolean> delete(Long id) {
        log.info("删除基础资料，id：{}", id);
        if (basicInfoMapper.selectById(id) == null) {
            return Result.error("基础资料不存在，id = " + id);
        }
        int rows = basicInfoMapper.deleteById(id);
        log.info("删除基础资料成功，id = {}", id);
        return Result.success(rows > 0);
    }

    /**
     * 校验 infoCategory 取值合法性。
     */
    private boolean isValidInfoCategory(String value) {
        return value == null
                || INFO_CATEGORY_MATERIAL.equals(value)
                || INFO_CATEGORY_EQUIPMENT.equals(value)
                || INFO_CATEGORY_WORKTYPE.equals(value);
    }

    /**
     * 校验 status 取值合法性。
     */
    private boolean isValidStatus(String value) {
        return value == null
                || STATUS_ACTIVE.equals(value)
                || STATUS_ARCHIVED.equals(value);
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
