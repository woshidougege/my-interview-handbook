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
     * @param directory  目录路径
     * @param file       文件流
     * @return 上传结果
     */
    FileUploadResponse uploadFile(String userEntityCode ,String directory, org.springframework.web.multipart.MultipartFile file);

    /**
     * 列出指定目录下的文件名列表
     *
     * @param directory  目录路径
     * @param recursive  是否递归
     * @return 文件名列表
     */
    List<String> listObjectNames( String directory, boolean recursive);
    
    /**
     * 获取文件的预签名URL
     *
     * @param filename   文件名
     * @return 文件的预签名URL
     */
    String getObjectURL(String filename);
}