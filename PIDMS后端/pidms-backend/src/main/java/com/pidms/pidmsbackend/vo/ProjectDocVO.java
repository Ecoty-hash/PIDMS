package com.pidms.pidmsbackend.vo;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class ProjectDocVO {
    private Long id;
    private String projectId;
    private String docName;
    private String docCategory;
    private String version;
    private String uploadBy;
    private String uploadTime;
    private String docType;
    private String docDescription;
//    private String attachments;
    private String createBy;
    private LocalDateTime createTime;
    private String updateBy;
    private LocalDateTime updateTime;
}
