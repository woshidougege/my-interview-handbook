package com.noah.superagent.controller;

import com.noah.superagent.common.dto.response.FileUploadResponse;
import com.noah.superagent.model.SSOUserInfo;
import com.noah.superagent.response.ApiResponse;
import com.noah.superagent.service.FileRepositoryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import static com.noah.superagent.util.UserContext.getCurrentUser;

/**
 * 文档库控制器
 *
 * @author 任相鹏
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
     * @param file 文件流
     * @param contextId 上下文ID（必填）
     * @param taskId 任务ID（可选）
     * @return 上传结果
     */
    @PostMapping(value = "/stream/put", consumes = "multipart/form-data")
    public ApiResponse<FileUploadResponse> uploadFile(
            @RequestParam(value = "file", required = false) MultipartFile file,
            @RequestParam(value = "contextId") String contextId,
            @RequestParam(value = "taskId", required = false) String taskId) throws IOException {

        log.info("接收到文件上传请求: fileIsNull={}, fileIsEmpty={}, contextId={}, taskId={}"
                , (file == null), (file == null ? "N/A" : file.isEmpty()), contextId, taskId);

        // 检查文件是否为空
        if (file == null || file.isEmpty()) {
            log.warn("文件上传请求中未包含有效文件");
            return ApiResponse.error(400, "文件不能为空");
        }

        // 检查contextId是否为空
        if (contextId == null || contextId.isEmpty()) {
            log.warn("上下文ID不能为空");
            return ApiResponse.error(400, "上下文ID不能为空");
        }

        // 获取用户实体编码
        String userEntityCode = getUserEntityCode();
        if (userEntityCode == null) {
            return ApiResponse.error(500, "无法获取用户信息");
        }

        // 构建目录结构: contextId/taskId/
        String directory = buildDirectoryPath(contextId, taskId);

        log.info("构建文件存储目录: {}", directory);

        // 调用服务层处理文件上传
        FileUploadResponse response = fileRepositoryService.uploadFile(userEntityCode, directory, file);

        // 检查上传是否成功
        if (response == null || "upload_failed".equals(response.getName())) {
            String errorMessage = (response != null && response.getUrl() != null) ? 
                response.getUrl() : "文件上传失败";
            log.error("文件上传失败: directory={}, 错误信息: {}", directory, errorMessage);
            return ApiResponse.error(500, errorMessage);
        }

        log.info("文件上传成功: directory={}, fileName={}", directory, response.getName());
        return ApiResponse.success("上传成功", response);
    }

    /**
     * 列出指定目录下的文件名列表
     *
     * @param contextId  上下文ID（可选）
     * @param taskId     任务ID（可选）
     * @param recursive  是否递归
     * @return 文件名列表
     */
    @GetMapping("/listObjectNames")
    public ApiResponse<List<String>> listObjectNames(
            @RequestParam(value = "contextId", required = false) String contextId,
            @RequestParam(value = "taskId", required = false) String taskId,
            @RequestParam(value = "recursive", defaultValue = "false") boolean recursive) {
        
        log.info("接收到文件列表请求: contextId={}, taskId={}, recursive={}", contextId, taskId, recursive);
        
        // 构建目录路径
        String directory = buildDirectoryPath(contextId, taskId);

        // 获取用户实体编码
        String userEntityCode = getUserEntityCode();
        if (userEntityCode == null) {
            return ApiResponse.error(500, "无法获取用户信息");
        }

        List<String> fileNames = fileRepositoryService.listObjectNames(directory, recursive, userEntityCode);

        log.info("文件列表获取成功: directory={}, fileCount={}", directory, 
                fileNames != null ? fileNames.size() : 0);
        return ApiResponse.success("获取成功", fileNames != null ? fileNames : List.of());
    }

    /**
     * 获取文件的预签名URL
     *
     * @param requestBody 请求体，包含filename参数
     * @return 文件的预签名URL
     */
    @PostMapping("/getObjectURL")
    public ApiResponse<String> getObjectURL(
            @RequestBody Map<String, String> requestBody) {
        
        // 参数校验
        if (requestBody == null) {
            log.warn("请求体不能为空");
            return ApiResponse.error(400, "请求体不能为空");
        }

        String filename = requestBody.get("filename");
        log.info("接收到获取文件URL请求: filename={}", filename);
        
        // 参数校验
        if (filename == null || filename.isEmpty()) {
            log.warn("文件名参数不能为空");
            return ApiResponse.error(400, "文件名参数不能为空");
        }

        // 获取用户实体编码
        String userEntityCode = getUserEntityCode();
        if (userEntityCode == null) {
            return ApiResponse.error(500, "无法获取用户信息");
        }

        String url = fileRepositoryService.getObjectURL(filename, userEntityCode);

        // 检查获取URL是否成功
        if (url == null || url.isEmpty()) {
            log.error("获取文件URL失败: filename={}", filename);
            return ApiResponse.error(500, "获取文件URL失败");
        }

        log.info("文件URL获取成功: filename={}, urlLength={}", filename, url.length());
        return ApiResponse.success("获取成功", url);
    }
    
    /**
     * 构建目录路径
     * 
     * @param contextId 上下文ID
     * @param taskId 任务ID
     * @return 目录路径
     */
    private String buildDirectoryPath(String contextId, String taskId) {
        StringBuilder directoryBuilder = new StringBuilder();
        
        if (contextId != null && !contextId.isEmpty()) {
            directoryBuilder.append(contextId);
            if (taskId != null && !taskId.isEmpty()) {
                directoryBuilder.append("/").append(taskId);
            }
            directoryBuilder.append("/");
        }
        
        return directoryBuilder.toString();
    }
    
    /**
     * 获取用户实体编码
     * 
     * @return 用户实体编码，如果获取失败返回null
     */
    private String getUserEntityCode() {
        // 获取当前用户信息
        SSOUserInfo currentUser = getCurrentUser();
        if (currentUser == null) {
            log.error("无法获取当前用户信息");
            return null;
        }

        String userId = currentUser.getUserId();
        String phonenumber = currentUser.getPhonenumber();

        // 验证必要用户信息
        if (userId == null || userId.isEmpty()) {
            log.error("用户ID为空");
            return null;
        }

        // 构建用户实体编码
        return "ENTITY_document_" + userId + "_" + phonenumber;
    }
}