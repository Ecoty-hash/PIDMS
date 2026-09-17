package com.pidms.pidmsbackend.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.pidms.pidmsbackend.entity.Project;
import com.pidms.pidmsbackend.vo.ProjectVO;
import org.apache.ibatis.annotations.Mapper;


@Mapper
public interface ProjectMapper extends BaseMapper<Project> {
}
