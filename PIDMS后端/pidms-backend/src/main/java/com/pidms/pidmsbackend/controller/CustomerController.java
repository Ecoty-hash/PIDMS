package com.pidms.pidmsbackend.controller;

import com.pidms.pidmsbackend.common.Result;
import com.pidms.pidmsbackend.dto.CustomerDTO;
import com.pidms.pidmsbackend.entity.PageInfo;
import com.pidms.pidmsbackend.entity.CustomerQueryParam;
import com.pidms.pidmsbackend.service.CustomerService;
import com.pidms.pidmsbackend.vo.CustomerVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

/**
 * 客户 接口
 */
@Slf4j
@RestController
@RequestMapping("/api/customers")
@Tag(name = "客户接口")
public class CustomerController {

    @Resource
    private CustomerService customerService;

    /**
     * 分页查询客户列表
     */
    @GetMapping
    @Operation(summary = "分页查询客户列表")
    public Result<PageInfo<CustomerVO>> pageQuery(CustomerQueryParam queryParam) {
        log.info("分页查询客户，参数：{}", queryParam);
        return customerService.pageQuery(queryParam);
    }

    /**
     * 查询客户详情
     */
    @GetMapping("/{id}")
    @Operation(summary = "查询客户详情")
    public Result<CustomerVO> getById(@PathVariable Long id) {
        log.info("查询客户详情，id：{}", id);
        return customerService.getById(id);
    }

    /**
     * 新增客户
     */
    @PostMapping
    @Operation(summary = "新增客户")
    public Result<Long> add(@RequestBody CustomerDTO dto) {
        log.info("新增客户，参数：{}", dto);
        return customerService.add(dto);
    }

    /**
     * 编辑客户
     */
    @PutMapping("/{id}")
    @Operation(summary = "编辑客户")
    public Result<Long> update(@PathVariable Long id, @RequestBody CustomerDTO dto) {
        log.info("编辑客户，id：{}，参数：{}", id, dto);
        return customerService.update(id, dto);
    }

    /**
     * 删除客户
     */
    @DeleteMapping("/{id}")
    @Operation(summary = "删除客户")
    public Result<Boolean> delete(@PathVariable Long id) {
        log.info("删除客户，id：{}", id);
        return customerService.delete(id);
    }
}
