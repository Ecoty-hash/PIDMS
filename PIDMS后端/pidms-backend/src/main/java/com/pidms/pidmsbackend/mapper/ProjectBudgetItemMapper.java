package com.pidms.pidmsbackend.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.injector.methods.InsertBatchSomeColumn;
import com.pidms.pidmsbackend.entity.BudgetItem;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface ProjectBudgetItemMapper
        extends BaseMapper<BudgetItem> {
}
