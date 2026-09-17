package com.pidms.pidmsbackend.dto;

import lombok.Data;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 公司文档 新增 / 编辑 请求体
 */
@Data
public class CompanyDocDTO {

    /** 主键ID（编辑时随请求体携带，与路径 /{id} 一致；新增不传） */
    private Long id;

    /** 文档编号 */
    private String docCode;

    /** 文档名称（列表展示，未填时取文档标题回填） */
    private String docName;

    /** 文档标题 */
    private String docTitle;

    /** 文档分类：regulation公司制度 / specification技术规范 / management管理文件 */
    private String docCategory;

    /** 版本号 */
    private String version;

    /** 发布部门 */
    private String publishDept;

    /** 发布日期 */
    private LocalDate publishDate;

    /** 文档说明 */
    private String docDescription;

    /** 上传人（由后端补全） */
    private String uploadBy;

    /** 上传时间（由后端补全） */
    private LocalDateTime uploadTime;

    /** 附件（文件名，多个以顿号分隔） */
    private String attachments;
}
