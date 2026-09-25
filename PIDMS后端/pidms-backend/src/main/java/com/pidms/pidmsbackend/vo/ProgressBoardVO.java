package com.pidms.pidmsbackend.vo;

import lombok.Data;

import java.util.List;

/**
 * 项目进度详情页 VO
 * <p>
 * project 直接复用 ProjectProgressCalculator.build() 的产物——项目基础信息（名称/编号/负责人/
 * 起止时间/状态）与进度汇总（实际进度、计划进度、差值、节点数、已完成数、逾期数）都在里面，
 * 保证本页与工作台、可视化管理三处口径完全一致，不各算一套。
 * </p>
 */
@Data
public class ProgressBoardVO {

    /** 项目基础信息 + 进度汇总（含节点数 / 已完成数 / 逾期数） */
    private ProjectProgressVO project;

    /**
     * 节点树（甘特图与列表共用）：只有顶层节点，子节点收在各自的 children 里，可多层嵌套。
     * level 已由后端算好，前端按层缩进、展开/折叠即可。
     */
    private List<ProgressNodeVO> nodes;

    /** 节点负责人候选：在职内部人员全量名单（该项目的负责人/成员排在前面，便于优先选中） */
    private List<String> responsibleOptions;

    /** 口径说明，页面脚注展示 */
    private String description;
}
