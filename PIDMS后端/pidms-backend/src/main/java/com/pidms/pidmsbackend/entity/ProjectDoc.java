package com.pidms.pidmsbackend.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("pm_project_doc")
public class ProjectDoc {

    /**
     * 主键ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 所属项目id
     */
    private String projectId;

    /**
     * 文档名称
     */
    private String docName;

    /**
     * 文档分类：施工图纸/技术资料/验收资料
     */
    private String docCategory;

    /**
     * 版本号
     */
    private String version;

    /**
     * 上传人
     */
    private String uploadBy;

    /**
     * 上传时间
     */
    private String uploadTime;

    /**
     * 文档类型
     */
    private String docType;

    /**
     * 文档说明
     */
    private String docDescription;

//    /**
//     * 附件（存储文件地址字符串，多个逗号分隔）
//     */
//    private String attachments;

    /**
     * 创建人
     */
    private String createBy;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 最后修改人
     */
    private String updateBy;

    /**
     * 更新时间
     */
    private LocalDateTime updateTime;
}
