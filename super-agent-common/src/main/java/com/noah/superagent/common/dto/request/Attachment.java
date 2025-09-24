package com.noah.superagent.common.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 附件信息类
 * 用于表示用户上传的附件信息
 */
@Data
@Schema(description = "附件信息")
public class Attachment {
    
    @Schema(description = "附件MIME类型", example = "text/html")
    private String mimeType;
    
    @Schema(description = "附件名称", example = "document.pdf")
    private String name;
    
    @Schema(description = "附件原始名称", example = "我的文档.pdf")
    private String originalName;
    
    @Schema(description = "附件URI", example = "/uploads/document.pdf")
    private String uri;
}