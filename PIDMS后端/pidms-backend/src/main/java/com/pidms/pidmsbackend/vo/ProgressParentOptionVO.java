package com.pidms.pidmsbackend.vo;

import lombok.Data;

/**
 * 上级节点候选（新增/编辑进度节点时的「上级节点」下拉）
 * <p>只返回同一个项目下的节点，已排除自身及其子孙（选了会形成循环层级）；
 * level 由后端算好，前端按层缩进展示。</p>
 */
@Data
public class ProgressParentOptionVO {

    /** 节点ID（提交时作为 parentId） */
    private Long id;

    /** 节点名称 */
    private String name;

    /** 层级深度（顶层为 0） */
    private Integer level;
}
