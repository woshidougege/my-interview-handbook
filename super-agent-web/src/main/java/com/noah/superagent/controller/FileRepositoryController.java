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
import static com.noah.superagent.util.UserContext.getCurrentUserId;

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
     * @return 上传结果
     */
    @PostMapping(value = "/stream/put", consumes = "multipart/form-data")
    public ApiResponse<FileUploadResponse> uploadFile(
            @RequestParam(value = "file", required = false) MultipartFile file) throws IOException {

        log.info("接收到文件上传请求: fileIsNull={}, fileIsEmpty={}"
                , (file == null), (file == null ? "N/A" : file.isEmpty()));

        // 如果文件不为空且非空文件，记录更多文件信息
        if (file != null && !file.isEmpty()) {
            log.info("文件信息: originalFilename={}, size={}, contentType={}",
                    file.getOriginalFilename(), file.getSize(), file.getContentType());
        }

        // 检查文件是否为空
        if (file == null || file.isEmpty()) {
            log.warn("文件上传请求中未包含有效文件");
            return ApiResponse.error(400, "文件不能为空");
        }


        String directory = "";

        // 获取当前用户信息
        SSOUserInfo currentUser = getCurrentUser();
        if (currentUser == null) {
            log.error("无法获取当前用户信息");
            return ApiResponse.error(500, "用户未登录或会话已过期");
        }
        
        String userId = currentUser.getUserId();
        String phonenumber = currentUser.getPhonenumber();
        
        // 验证必要用户信息
        if (userId == null || userId.isEmpty()) {
            log.error("用户ID为空");
            return ApiResponse.error(500, "用户信息不完整");
        }

        // 构建用户实体编码
        String userEntityCode = "ENTITY_document_" + userId + "_" + phonenumber;


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
     * @param directory  目录路径
     * @param recursive  是否递归
     * @return 文件名列表
     */
    @GetMapping("/listObjectNames")
    public ApiResponse<List<String>> listObjectNames(
            @RequestParam("directory") String directory,
            @RequestParam(value = "recursive", defaultValue = "false") boolean recursive) {
        
        log.info("接收到文件列表请求: directory={}, recursive={}", directory, recursive);
        
        // 参数校验
        if (directory == null) {
            log.warn("目录参数不能为空");
            return ApiResponse.error(400, "目录参数不能为空");
        }

        List<String> fileNames = fileRepositoryService.listObjectNames(directory, recursive);

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

        String url = fileRepositoryService.getObjectURL(filename);

        // 检查获取URL是否成功
        if (url == null || url.isEmpty()) {
            log.error("获取文件URL失败: filename={}", filename);
            return ApiResponse.error(500, "获取文件URL失败");
        }

        log.info("文件URL获取成功: filename={}, urlLength={}", filename, url.length());
        return ApiResponse.success("获取成功", url);
    }
}