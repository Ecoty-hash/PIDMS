package com.pidms.pidmsbackend.service;

import com.pidms.pidmsbackend.dto.ProjectDocQueryDTO;
import com.pidms.pidmsbackend.entity.PageInfo;
import com.pidms.pidmsbackend.entity.ProjectDocQueryParam;
import com.pidms.pidmsbackend.vo.ProjectDocVO;

public interface ProjectDocService {

    PageInfo<ProjectDocVO> queryPage(ProjectDocQueryParam queryParam);
}
