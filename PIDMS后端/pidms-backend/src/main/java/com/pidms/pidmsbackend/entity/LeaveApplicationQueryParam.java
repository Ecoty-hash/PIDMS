package com.pidms.pidmsbackend.entity;

import lombok.Data;
import java.time.LocalDate;

/**
 * 请假申请 分页查询参数
 */
@Data
public class LeaveApplicationQueryParam {

    /** 页码，默认 1 */
    private Long page;

    /** 每页条数，默认 20 */
    private Long pageSize;

    /** 关键字模糊搜索 */
    private String keyword;

    /** applyNo（高级搜索，精确匹配） */
    private String applyNo;

    /** applicant（高级搜索，精确匹配） */
    private String applicant;

    /** leaveType（高级搜索，精确匹配） */
    private String leaveType;

    /** approvalStatus（高级搜索，精确匹配） */
    private String approvalStatus;

    /** 开始日期下限（高级搜索，start_date >= 该值） */
    private LocalDate startDate;

    /** 结束日期上限（高级搜索，end_date <= 该值） */
    private LocalDate endDate;
}
