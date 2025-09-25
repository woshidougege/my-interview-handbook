package com.noah.superagent.service;

import com.noah.superagent.common.dto.response.FileUploadResponse;

import java.util.List;

/**
 * 文件仓库服务接口
 *
 * @author 任相鹏
 * @since 1.0.0
 */
public interface FileRepositoryService {
    /**
     * 上传文件
     *
     * @param userEntityCode 用户实体编码
     * @param directory      目录路径
     * @param file           文件流
     * @return 上传结果
     */
    FileUploadResponse uploadFile(String userEntityCode, String directory, org.springframework.web.multipart.MultipartFile file);

    /**
     * 列出指定目录下的文件名列表
     *
     * @param directory      目录路径
     * @param recursive      是否递归
     * @param userEntityCode 用户实体编码
     * @return 文件名列表
     */
    List<String> listObjectNames(String directory, boolean recursive, String userEntityCode);
    
    /**
     * 获取文件的预签名URL
     *
     * @param filename       文件名
     * @param userEntityCode 用户实体编码
     * @return 文件的预签名URL
     */
    String getObjectURL(String filename, String userEntityCode);

    /**
     * 构建目录路径
     * 
     * @param contextId 上下文ID
     * @param taskId 任务ID
     * @return 目录路径
     */
    String buildDirectoryPath(String contextId, String taskId);
    

}