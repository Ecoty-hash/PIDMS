package com.pidms.pidmsbackend.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.pidms.pidmsbackend.common.Result;
import com.pidms.pidmsbackend.common.UserContext;
import com.pidms.pidmsbackend.convert.CompanyDocConvert;
import com.pidms.pidmsbackend.dto.CompanyDocDTO;
import com.pidms.pidmsbackend.entity.CompanyDoc;
import com.pidms.pidmsbackend.entity.CompanyDocQueryParam;
import com.pidms.pidmsbackend.entity.PageInfo;
import com.pidms.pidmsbackend.mapper.CompanyDocMapper;
import com.pidms.pidmsbackend.service.CompanyDocService;
import com.pidms.pidmsbackend.vo.CompanyDocVO;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 公司文档 服务实现
 */
@Slf4j
@Service
public class CompanyDocServiceImpl implements CompanyDocService {

    /** docCategory：regulation */
    private static final String DOC_CATEGORY_REGULATION = "regulation";
    /** docCategory：specification */
    private static final String DOC_CATEGORY_SPECIFICATION = "specification";
    /** docCategory：management */
    private static final String DOC_CATEGORY_MANAGEMENT = "management";

    @Resource
    private CompanyDocMapper companyDocMapper;

    @Resource
    private CompanyDocConvert companyDocConvert;

    @Override
    public Result<PageInfo<CompanyDocVO>> pageQuery(CompanyDocQueryParam queryParam) {
        long pageNum = queryParam.getPage() == null ? 1 : queryParam.getPage();
        long pageSize = queryParam.getPageSize() == null ? 20 : queryParam.getPageSize();
        log.info("分页查询公司文档：pageNum = {}, pageSize = {}, 参数 = {}", pageNum, pageSize, queryParam);

        QueryWrapper<CompanyDoc> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq(StringUtils.hasText(queryParam.getDocCategory()), "doc_category", queryParam.getDocCategory());
        queryWrapper.eq(StringUtils.hasText(queryParam.getPublishDept()), "publish_dept", queryParam.getPublishDept());
        queryWrapper.like(StringUtils.hasText(queryParam.getDocTitle()), "doc_title", queryParam.getDocTitle());

        if (StringUtils.hasText(queryParam.getKeyword())) {
            String keyword = queryParam.getKeyword().trim();
            queryWrapper.and(w -> w
                        .like("doc_code", keyword)
                        .or().like("doc_name", keyword)
                        .or().like("doc_title", keyword));
        }
        queryWrapper.orderByAsc("id");

        Page<CompanyDoc> page = new Page<>(pageNum, pageSize);
        Page<CompanyDoc> resultPage = companyDocMapper.selectPage(page, queryWrapper);
        List<CompanyDoc> records = resultPage.getRecords();
        log.info("分页查询公司文档结果条数：{}", records.size());

        PageInfo<CompanyDocVO> pageInfo = new PageInfo<>();
        pageInfo.setPage((int) resultPage.getCurrent());
        pageInfo.setPageSize((int) resultPage.getSize());
        pageInfo.setTotal(resultPage.getTotal());
        pageInfo.setPages((int) ((resultPage.getTotal() + pageSize - 1) / pageSize));
        pageInfo.setList(companyDocConvert.entityListToVoList(records));
        return Result.success(pageInfo);
    }

    @Override
    public Result<CompanyDocVO> getById(Long id) {
        log.info("查询公司文档详情，id：{}", id);
        CompanyDoc entity = companyDocMapper.selectById(id);
        if (entity == null) {
            return Result.error("公司文档不存在，id = " + id);
        }
        return Result.success(companyDocConvert.entityToVo(entity));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<Long> add(CompanyDocDTO dto) {
        log.info("新增公司文档，参数：{}", dto);
        if (!StringUtils.hasText(dto.getDocCode())) {
            return Result.error("文档编号不能为空");
        }
        if (!StringUtils.hasText(dto.getDocTitle())) {
            return Result.error("文档标题不能为空");
        }
        if (!StringUtils.hasText(dto.getDocCategory())) {
            return Result.error("文档分类不能为空");
        }
        if (StringUtils.hasText(dto.getDocCategory()) && !isValidDocCategory(dto.getDocCategory())) {
            return Result.error("文档分类取值不合法，仅支持 regulation / specification / management");
        }
        if (StringUtils.hasText(dto.getDocCode())) {
            Long dupCount = companyDocMapper.selectCount(Wrappers.<CompanyDoc>lambdaQuery()
                    .eq(CompanyDoc::getDocCode, dto.getDocCode()));
            if (dupCount != null && dupCount > 0) {
                return Result.error("文档编号已存在：" + dto.getDocCode());
            }
        }
        CompanyDoc entity = companyDocConvert.dtoToEntity(dto);
        if (!StringUtils.hasText(entity.getDocName())) {
            entity.setDocName(entity.getDocTitle());
        }
        entity.setUploadBy(currentOperator());
        entity.setUploadTime(LocalDateTime.now());
        entity.setCreateBy(currentOperator());
        companyDocMapper.insert(entity);
        log.info("新增公司文档成功，id = {}", entity.getId());
        return Result.success(entity.getId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<Long> update(Long id, CompanyDocDTO dto) {
        log.info("编辑公司文档，id：{}，参数：{}", id, dto);
        CompanyDoc exist = companyDocMapper.selectById(id);
        if (exist == null) {
            return Result.error("公司文档不存在，id = " + id);
        }
        if (dto.getDocCode() != null && !StringUtils.hasText(dto.getDocCode())) {
            return Result.error("文档编号不能为空");
        }
        if (dto.getDocTitle() != null && !StringUtils.hasText(dto.getDocTitle())) {
            return Result.error("文档标题不能为空");
        }
        if (dto.getDocCategory() != null && !StringUtils.hasText(dto.getDocCategory())) {
            return Result.error("文档分类不能为空");
        }
        if (StringUtils.hasText(dto.getDocCategory()) && !isValidDocCategory(dto.getDocCategory())) {
            return Result.error("文档分类取值不合法，仅支持 regulation / specification / management");
        }
        if (StringUtils.hasText(dto.getDocCode())) {
            Long dupCount = companyDocMapper.selectCount(Wrappers.<CompanyDoc>lambdaQuery()
                    .eq(CompanyDoc::getDocCode, dto.getDocCode())
                    .ne(CompanyDoc::getId, id));
            if (dupCount != null && dupCount > 0) {
                return Result.error("文档编号已存在：" + dto.getDocCode());
            }
        }
        if (!StringUtils.hasText(dto.getDocName()) && StringUtils.hasText(dto.getDocTitle())) {
            dto.setDocName(dto.getDocTitle());
        }
        CompanyDoc entity = companyDocConvert.dtoToEntity(dto);
        entity.setId(id);
        entity.setUpdateBy(currentOperator());
        // updateById 仅更新非空字段：未传字段保留原值
        companyDocMapper.updateById(entity);
        log.info("编辑公司文档成功，id = {}", id);
        return Result.success(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<Boolean> delete(Long id) {
        log.info("删除公司文档，id：{}", id);
        if (companyDocMapper.selectById(id) == null) {
            return Result.error("公司文档不存在，id = " + id);
        }
        int rows = companyDocMapper.deleteById(id);
        log.info("删除公司文档成功，id = {}", id);
        return Result.success(rows > 0);
    }

    /**
     * 校验 docCategory 取值合法性。
     */
    private boolean isValidDocCategory(String value) {
        return value == null
                || DOC_CATEGORY_REGULATION.equals(value)
                || DOC_CATEGORY_SPECIFICATION.equals(value)
                || DOC_CATEGORY_MANAGEMENT.equals(value);
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
