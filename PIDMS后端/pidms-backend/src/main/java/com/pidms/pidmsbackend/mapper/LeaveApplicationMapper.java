package com.pidms.pidmsbackend.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.pidms.pidmsbackend.entity.LeaveApplication;
import org.apache.ibatis.annotations.Mapper;

/**
 * 请假申请 Mapper
 */
@Mapper
public interface LeaveApplicationMapper extends BaseMapper<LeaveApplication> {
}
