package com.noah.superagent.common.dto.response;

import lombok.Data;

/**
 * 文件上传响应对象
 *
 * @author System
 * @since 1.0.0
 */
@Data
public class FileUploadResponse {
    private String name;
    private String url;
}