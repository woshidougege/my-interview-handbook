package com.noah.superagent.model;

import lombok.Data;

/**
 * 聊天文件项
 *
 * @author 任相鹏
 * @since 1.0.0
 */
@Data
public class ChatFileItem {
    /**
     * 文件完整路径名
     */
    private String name;
    
    /**
     * 文件简单名称
     */
    private String fileName;
    
    /**
     * 文件显示名称
     */
    private String displayName;
    
    /**
     * 文件URI
     */
    private String uri;
}