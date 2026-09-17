package com.pidms.pidmsbackend.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.pidms.pidmsbackend.convert.ProjectConvertMapper;
import com.pidms.pidmsbackend.dto.ProjectSaveDTO;
import com.pidms.pidmsbackend.entity.PageInfo;
import com.pidms.pidmsbackend.entity.Project;
import com.pidms.pidmsbackend.entity.ProjectQueryParam;
import com.pidms.pidmsbackend.mapper.ProjectMapper;
import com.pidms.pidmsbackend.service.ProjectService;
import com.pidms.pidmsbackend.vo.ProjectVO;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;

@Slf4j
@Service
public class ProjectServiceImpl extends ServiceImpl<ProjectMapper, Project> implements ProjectService{


    @Resource
    private ProjectMapper projectMapper;

    // MapStruct转换器实例
    private final ProjectConvertMapper convertMapper = ProjectConvertMapper.INSTANCE;

    /**
     * 项目分页查询
     * @param param 前端传入查询分页参数
     * @return 分页结果数据
     */
    @Override
    public PageInfo<ProjectVO> pageQuery(ProjectQueryParam param) {
        // ============ 打印前端传入的原始参数日志 ============
        log.info("[项目分页查询] 收到前端请求参数:{}",param);

        // 分页默认值，页码为空默认第1页，页大小为空默认20条
        Integer pageNum = param.getPage() == null ? 1 : param.getPage();
        Integer pageSize = param.getPageSize() == null ? 20 : param.getPageSize();

        log.info("[项目分页查询] 解析后页码:{},每页条数:{}",pageNum,pageSize);

        //构建查询条件对象
        QueryWrapper<Project> wrapper = new QueryWrapper<>();

        // 项目名称模糊查询
        wrapper.like(param.getProjectName() != null, "project_name", param.getProjectName());
        // 归档状态精确匹配
        wrapper.eq(param.getArchiveStatus() != null, "archive_status", param.getArchiveStatus());
        // 项目状态精确匹配
        wrapper.eq(param.getProjectStatus() != null, "project_status", param.getProjectStatus());

        //开工日期范围查询 大于等于开始时间
        wrapper.ge(param.getStartDateBegin() != null, "start_date", param.getStartDateBegin());
        //开工日期 小于等于结束时间
        wrapper.le(param.getStartDateEnd() != null, "start_date", param.getStartDateEnd());

        //竣工日期范围
        wrapper.ge(param.getCompletionDateBegin() != null, "completion_date", param.getCompletionDateBegin());
        wrapper.le(param.getCompletionDateEnd() != null, "completion_date", param.getCompletionDateEnd());

        //keyword 关键字搜索，多字段模糊匹配：项目名称 /备注
        if(param.getKeyword() != null){
            log.info("[项目分页查询] 开启关键字模糊搜索,keyword={}",param.getKeyword());
            wrapper.and(w-> w.like("project_name",param.getKeyword())
                    .or().like("remark",param.getKeyword()));
        }

        // Mybatis‑Plus 创建分页对象
        Page<Project> mpPage = new Page<>(pageNum, pageSize);
        log.info("[项目分页查询] 开始执行数据库分页SQL查询");

        //执行数据库查询
        Page<Project> resultPage = projectMapper.selectPage(mpPage, wrapper);

        log.info("[项目分页查询] SQL查询完成,总数据量:{},当前页返回条数:{}",
                resultPage.getTotal(),
                resultPage.getRecords().size());

        //组装自定义返回分页对象，返回给前端
        PageInfo<ProjectVO> pageInfo = new PageInfo<>();
        pageInfo.setPage((int) resultPage.getCurrent());
        pageInfo.setPageSize((int) resultPage.getSize());
        pageInfo.setTotal(resultPage.getTotal());

        //【修复】实体列表 转换 VO列表
        pageInfo.setList(convertMapper.entityListToVoList(resultPage.getRecords()));

        log.info("[项目分页查询] 分页接口处理完毕，准备返回数据给前端");
        return pageInfo;
    }

    @Override
    public ProjectVO getProjectDetailById(Long id) {
        Project project = projectMapper.selectById(id);
        if(project == null){
            throw new RuntimeException("项目不存在");
        }
        //单对象转换
        return convertMapper.entityToVo(project);
    }

    @Override
    public void addProject(ProjectSaveDTO dto, String admin) {
        //DTO转数据库实体
        Project project = convertMapper.dtoToEntity(dto);
        //补审计字段
        project.setCreateBy(admin);
        projectMapper.insert(project);
    }

    @Override
    public void updateProject(ProjectSaveDTO dto, String operateUser) {
        Project project = convertMapper.dtoToEntity(dto);
        project.setUpdateBy(operateUser);
        projectMapper.updateById(project);
    }

    @Override
    public void deleteById(Long id,String operateUser) {
        projectMapper.deleteById(id);
    }

    @Override
    public void updateProjectStatus(Long id, String operateUser) {
        // 查询数据库完整数据
        Project project = projectMapper.selectById(id);
        if(project == null){
            throw new RuntimeException("项目不存在");
        }
       //判断是否归档，是则取消归档，否则进行归档。数据库约定：archived已归档 / unarchived未归档
        String oldStatus = project.getArchiveStatus();
        if("archived".equals(oldStatus)){
            project.setArchiveStatus("unarchived");
        }else{
            project.setArchiveStatus("archived");
        }
        project.setUpdateBy(operateUser);
        projectMapper.updateById(project);
    }
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void batchNumber(Long[] ids, String operateUsername) {
        if(ids == null || ids.length == 0){
            log.warn("[批量申请项目编号] id数组为空，直接返回");
            return;
        }
        List<Project> projectList = projectMapper.selectBatchIds(Arrays.asList(ids));
        if(projectList.isEmpty()){
            log.warn("[批量申请项目编号] 查询不到选中项目");
            return;
        }

        String today = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String prefix = "XM-" + today + "-";

        // 查询今天所有已经生成的编号
        LambdaQueryWrapper<Project> wrapper = new LambdaQueryWrapper<>();
        wrapper.likeRight(Project::getProjectCode, prefix);
        List<Project> todayCodeList = projectMapper.selectList(wrapper);

        int currentMaxSerial = 0;
        for (Project item : todayCodeList) {
            String code = item.getProjectCode();
            if(StrUtil.isBlank(code) || code.length() < prefix.length() + 3){
                continue;
            }
            //截取末尾3位流水
            String serialStr = code.substring(code.length() - 3);
            try{
                int serialNum = Integer.parseInt(serialStr);
                if(serialNum > currentMaxSerial){
                    currentMaxSerial = serialNum;
                }
            }catch (NumberFormatException e){
                //末尾不是数字，跳过脏数据
                log.error("[批量申请编号] 项目编号{}末尾流水号解析失败",code);
            }
        }

        int updateCount = 0;
        //循环生成编号
        for (Project project : projectList) {
            if(StrUtil.isBlank(project.getProjectCode())){
                currentMaxSerial++;
                String projectNumber = generateProjectNumber(today, currentMaxSerial);
                project.setProjectCode(projectNumber);
                updateCount++;
            }
        }
        //批量更新
        if(updateCount > 0){
            this.updateBatchById(projectList);
            log.info("[批量申请项目编号] 操作员:{},成功生成{}条编号",operateUsername,updateCount);
        }else{
            log.info("[批量申请项目编号] 选中项目均已有编号，无需生成");
        }
    }

    /**
     * XM‑20260902‑001
     */
    private String generateProjectNumber(String dateStr, int serial){
        return "XM-" + dateStr + "-" + String.format("%03d", serial);
    }

//    @Override
//    public void batchNumber(Long[] ids, String operateUsername) {
//        //查询所有选中id的项目
//        List<Project> projectList = projectMapper.selectBatchIds(Arrays.asList(ids));
//        int serial = 1;
//        for(Project project : projectList)
//        {
//         //给没有编号的项目补编号，并逐个更新数据库
//            if(project.getProjectCode() == null || project.getProjectCode().isEmpty()){
//                //调用生成编号方法
//                String projectNumber = generateProjectNumber(serial);
//                project.setProjectCode(projectNumber);
//                projectMapper.updateById(project);
//                serial++;
//            }
//        }
//
//    }
//    private String generateProjectNumber(int serial){
//        String dateStr = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
//        // 你可以根据自己的编号规则自行修改
//        return "XM-" + dateStr + "-" + String.format("%03d", serial);
//    }
}
