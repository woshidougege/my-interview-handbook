package com.noah.superagent.controller;

import com.noah.superagent.response.ApiResponse;
import com.noah.superagent.service.FileRepositoryService;
import com.noah.superagent.common.dto.response.FileUploadResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * 文档库控制器
 *
 * @author System
 * @since 1.0.0
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/file-repository")
@RequiredArgsConstructor
public class FileRepositoryController {

    private final FileRepositoryService fileRepositoryService;

    /**
     * 文件上传接口
     *
     * @param params 参数（JSON格式字符串，包含entityCode和directory）
     * @param file   文件流
     * @return 上传结果
     */
    @PostMapping(value = "/stream/put", consumes = "multipart/form-data")
    public ApiResponse<FileUploadResponse> uploadFile(
            @RequestParam(value = "params", required = false) String params,
            @RequestParam(value = "file", required = false) MultipartFile file) throws IOException {
        
        log.info("接收到文件上传请求: params={}, fileIsNull={}, fileIsEmpty={}", 
                params, (file == null), (file == null ? "N/A" : file.isEmpty()));
        
        // 如果文件不为空，记录更多文件信息
        if (file != null) {
            log.info("文件信息: originalFilename={}, size={}, contentType={}", 
                    file.getOriginalFilename(), file.getSize(), file.getContentType());
        }
        
        // 检查文件是否为空
        if (file == null || file.isEmpty()) {
            log.warn("文件上传失败：文件为空，params={}", params);
            return ApiResponse.error(400, "文件不能为空");
        }
        
        // 解析params参数
        String entityCode = "default_entity";
        String directory = "";
        
        try {
            if (params != null && !params.isEmpty()) {
                // 解析JSON格式的params参数
                Map<String, Object> paramMap = new com.fasterxml.jackson.databind.ObjectMapper()
                    .readValue(params, new com.fasterxml.jackson.core.type.TypeReference<Map<String, Object>>() {});
                
                entityCode = (String) paramMap.getOrDefault("entityCode", entityCode);
                directory = (String) paramMap.getOrDefault("directory", directory);
            }
        } catch (Exception e) {
            log.warn("解析params参数失败: {}", e.getMessage());
        }
        
        log.info("解析后的参数: entityCode={}, directory={}", entityCode, directory);
        
        // 调用服务层处理文件上传
        FileUploadResponse response = fileRepositoryService.uploadFile(entityCode, directory, file);

        // 检查上传是否成功
        if (response.getName().equals("upload_failed")) {
            log.error("文件上传失败: entityCode={}, directory={}", entityCode, directory);
            return ApiResponse.error(500, "文件上传失败");
        }

        log.info("文件上传成功: entityCode={}, directory={}, fileName={}", entityCode, directory, response.getName());
        return ApiResponse.success("上传成功", response);
    }



    /**
     * 列出指定目录下的文件名列表
     *
     * @param entityCode 实体编码
     * @param directory  目录路径
     * @param recursive  是否递归
     * @return 文件名列表
     */
    @GetMapping("/listObjectNames")
    public ApiResponse<List<String>> listObjectNames(
            @RequestParam("entityCode") String entityCode,
            @RequestParam("directory") String directory,
            @RequestParam(value = "recursive", defaultValue = "false") boolean recursive) {

        log.info("接收到文件列表请求: entityCode={}, directory={}, recursive={}", entityCode, directory, recursive);

        List<String> fileNames = fileRepositoryService.listObjectNames(entityCode, directory, recursive);

        log.info("文件列表获取成功: entityCode={}, directory={}, fileCount={}", entityCode, directory, fileNames.size());
        return ApiResponse.success("获取成功", fileNames);
    }
    
    /**
     * 获取文件的预签名URL
     *
     * @param entityCode 实体编码
     * @param requestBody 请求体，包含filename参数
     * @return 文件的预签名URL
     */
    @PostMapping("/getObjectURL")
    public ApiResponse<String> getObjectURL(
            @RequestParam("entityCode") String entityCode,
            @RequestBody Map<String, String> requestBody) {
        
        String filename = requestBody.get("filename");
        log.info("接收到获取文件URL请求: entityCode={}, filename={}", entityCode, filename);

        String url = fileRepositoryService.getObjectURL(entityCode, filename);
        
        // 检查获取URL是否成功
        if (url.isEmpty()) {
            log.error("获取文件URL失败: entityCode={}, filename={}", entityCode, filename);
            return ApiResponse.error(500, "获取文件URL失败");
        }

        log.info("文件URL获取成功: entityCode={}, filename={}, url={}", entityCode, filename, url);
        return ApiResponse.success("获取成功", url);
    }
}