package com.noah.superagent.common.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 文件存储配置属性
 * 对应 application.yml 中的 file-repository 配置
 *
 * @author 任相鹏
 * @since 1.0.0
 */
@Data
@ConfigurationProperties(prefix = "file-repository")
public class FileRepositoryProperties {

    /**
     * 文件存储服务基础URL
     */
    private String baseUrl;

    /**
     * 文件上传路径
     */
    private String uploadPath;

    /**
     * 列出对象名称路径
     */
    private String listObjectNamesPath;

    /**
     * 获取对象URL路径
     */
    private String getObjectUrlPath;
}
