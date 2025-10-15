package com.noah.superagent.controller;

import com.noah.superagent.common.dto.response.FileUploadResponse;
import com.noah.superagent.model.SSOUserInfo;
import com.noah.superagent.response.ApiResponse;
import com.noah.superagent.service.FileRepositoryService;
import com.noah.superagent.util.UserEntityCodeUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.net.URL;
import java.net.URLConnection;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

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
     * @param file      文件流
     * @param directory 目录
     * @return 上传结果
     */
    @PostMapping(value = "/put", consumes = "multipart/form-data")
    public ApiResponse<FileUploadResponse> uploadFile(
            @RequestParam(value = "file", required = false) MultipartFile file,
            @RequestParam(value = "directory", required = false) String directory) throws IOException {

        log.info("接收到文件上传请求: fileIsNull={}, fileIsEmpty={},  dir={}"
                , (file == null), (file == null ? "N/A" : file.isEmpty()), directory);

        // 检查文件是否为空
        if (file == null || file.isEmpty()) {
            log.warn("文件上传请求中未包含有效文件");
            return ApiResponse.error(400, "文件不能为空");
        }


        try {
            // 获取用户实体编码
            String userEntityCode = validateAndGetUserEntityCode();

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
        } catch (IllegalStateException e) {
            log.error("无法获取用户信息", e);
            return ApiResponse.error(500, "无法获取用户信息: " + e.getMessage());
        } catch (Exception e) {
            log.error("文件上传过程中发生异常", e);
            return ApiResponse.error(500, "文件上传失败: " + e.getMessage());
        }
    }

    /**
     * 文件上传接口
     *
     * @param file      文件流
     * @param contextId 上下文ID（必填）
     * @param taskId    任务ID（可选）
     * @return 上传结果
     */
    @PostMapping(value = "/stream/put", consumes = "multipart/form-data;charset=UTF-8")
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

        try {
            // 获取用户实体编码
            String userEntityCode = validateAndGetUserEntityCode();

            // 构建目录结构: contextId/taskId
            String directory = fileRepositoryService.buildDirectoryPath(contextId, taskId);

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
        } catch (IllegalStateException e) {
            log.error("无法获取用户信息", e);
            return ApiResponse.error(500, "无法获取用户信息: " + e.getMessage());
        } catch (Exception e) {
            log.error("文件上传过程中发生异常", e);
            return ApiResponse.error(500, "文件上传失败: " + e.getMessage());
        }
    }

    /**
     * 列出指定目录下的文件名列表
     *
     * @param contextId 上下文ID（可选）
     * @param taskId    任务ID（可选）
     * @param recursive 是否递归
     * @return 文件名列表
     */
    @GetMapping("/listObjectNames")
    public ApiResponse<List<String>> listObjectNames(
            @RequestParam(value = "contextId", required = false) String contextId,
            @RequestParam(value = "taskId", required = false) String taskId,
            @RequestParam(value = "recursive", defaultValue = "false") boolean recursive) {

        log.info("接收到文件列表请求: contextId={}, taskId={}, recursive={}", contextId, taskId, recursive);

        try {
            // 获取用户实体编码
            String userEntityCode = validateAndGetUserEntityCode();
            log.debug("获取到用户实体编码: {}", userEntityCode);

            // 构建目录路径
            String directory = fileRepositoryService.buildDirectoryPath(contextId, taskId);
            log.debug("构建的目录路径: {}", directory);

            if (directory == null || directory.trim().isEmpty()) {
                log.warn("构建的目录路径为空或无效");
                return ApiResponse.error(400, "目录路径不能为空");
            }

            List<String> fileNames = fileRepositoryService.listObjectNames(directory, recursive, userEntityCode);

            int fileCount = fileNames != null ? fileNames.size() : 0;
            log.info("文件列表获取成功: directory={}, fileCount={}", directory, fileCount);
            
            if (fileCount == 0) {
                log.debug("指定目录中没有文件: {}", directory);
            }
            
            return ApiResponse.success("获取成功", fileNames != null ? fileNames : List.of());
        } catch (IllegalStateException e) {
            log.error("无法获取用户信息", e);
            return ApiResponse.error(500, "无法获取用户信息: " + e.getMessage());
        } catch (IllegalArgumentException e) {
            log.error("参数非法: contextId={}, taskId={}", contextId, taskId, e);
            return ApiResponse.error(400, "参数错误: " + e.getMessage());
        } catch (SecurityException e) {
            log.error("访问被拒绝: 用户无权访问目录: contextId={}, taskId={}", contextId, taskId, e);
            return ApiResponse.error(403, "无权访问该目录");
        } catch (Exception e) {
            log.error("获取文件列表过程中发生未知异常: contextId={}, taskId={}", contextId, taskId, e);
            return ApiResponse.error(500, "获取文件列表失败: " + e.getMessage());
        }
    }


    /**
     * 根据文件名列表下载文件
     * 如果只有一个文件，则直接下载该文件；如果有多个文件，则打包成ZIP下载
     *
     * @param fileNames 文件名列表
     * @param response  HttpServletResponse对象
     */
    @PostMapping("/downloadByNames")
    public void downloadByNames(
            @RequestBody List<String> fileNames,
            HttpServletResponse response) {
        log.info("接收到按文件名下载请求: fileNames={}", fileNames);

        try {
            // 获取用户实体编码
            String userEntityCode = validateAndGetUserEntityCode();

            if (fileNames == null || fileNames.isEmpty()) {
                log.warn("文件名列表为空");
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                response.getWriter().write("文件名列表不能为空");
                return;
            }

            // 如果只有一个文件，直接下载
            if (fileNames.size() == 1) {
                String fileName = fileNames.get(0);
                downloadSingleFile(fileName, userEntityCode, response);
            } else {
                // 如果有多个文件，打包下载
                downloadMultipleFilesAsZip(fileNames, userEntityCode, response);
            }
        } catch (Exception e) {
            log.error("按文件名下载过程中发生异常", e);
            try {
                response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                response.getWriter().write("下载失败: " + e.getMessage());
            } catch (IOException ioException) {
                log.error("无法向响应写入错误信息", ioException);
            }
        }
    }

    /**
     * 下载单个文件
     *
     * @param fileName       文件名
     * @param userEntityCode 用户实体编码
     * @param response       HttpServletResponse对象
     * @throws IOException IO异常
     */
    private void downloadSingleFile(String fileName, String userEntityCode, HttpServletResponse response) throws IOException {
        try {
            // 获取文件的预签名URL
            String fileUrl = fileRepositoryService.getObjectURL(fileName, userEntityCode);

            if (fileUrl == null || fileUrl.isEmpty()) {
                log.warn("无法获取文件的URL: fileName={}", fileName);
                response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                response.getWriter().write("无法获取文件的下载链接: " + fileName);
                return;
            }

            // 下载文件
            URL url = new URL(fileUrl);
            try (InputStream in = url.openStream()) {
                // 设置响应头
                response.setContentType("application/octet-stream");
                response.setHeader("Content-Disposition", "attachment; filename=" + fileName);

                // 将文件内容写入响应
                IOUtils.copy(in, response.getOutputStream());
                response.flushBuffer();
            }
        } catch (Exception e) {
            log.error("下载单个文件时发生异常: fileName={}", fileName, e);
            throw e;
        }
    }

    /**
     * 下载多个文件并打包成ZIP
     *
     * @param fileNames      文件名列表
     * @param userEntityCode 用户实体编码
     * @param response       HttpServletResponse对象
     * @throws IOException IO异常
     */
    private void downloadMultipleFilesAsZip(List<String> fileNames, String userEntityCode, HttpServletResponse response) throws IOException {
        try (ZipOutputStream zipOut = new ZipOutputStream(response.getOutputStream())) {
            // 设置响应头
            response.setContentType("application/zip");
            response.setHeader("Content-Disposition", "attachment; filename=download.zip");

            // 下载并添加每个文件到ZIP
            for (String fileName : fileNames) {
                try {
                    // 获取文件的预签名URL
                    String fileUrl = fileRepositoryService.getObjectURL(fileName, userEntityCode);

                    if (fileUrl == null || fileUrl.isEmpty()) {
                        log.warn("无法获取文件的URL: fileName={}", fileName);
                        continue;
                    }

                    // 下载文件
                    URL url = new URL(fileUrl);
                    try (InputStream in = url.openStream()) {
                        // 添加文件到ZIP
                        ZipEntry zipEntry = new ZipEntry(fileName);
                        zipOut.putNextEntry(zipEntry);

                        byte[] buffer = new byte[1024];
                        int len;
                        while ((len = in.read(buffer)) > 0) {
                            zipOut.write(buffer, 0, len);
                        }
                        zipOut.closeEntry();
                    }
                } catch (Exception e) {
                    log.error("处理文件时发生异常: fileName={}", fileName, e);
                    // 继续处理其他文件
                }
            }

            zipOut.finish();
            log.info("多文件打包下载完成: fileCount={}", fileNames.size());
        } catch (Exception e) {
            log.error("多文件打包下载过程中发生异常", e);
            throw e;
        }
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

        try {
            // 获取用户实体编码
            String userEntityCode = validateAndGetUserEntityCode();

            String url = fileRepositoryService.getObjectURL(filename, userEntityCode);

            // 检查获取URL是否成功
            if (url == null || url.isEmpty()) {
                log.error("获取文件URL失败: filename={}", filename);
                return ApiResponse.error(500, "获取文件URL失败");
            }

            log.info("文件URL获取成功: filename={}, urlLength={}", filename, url.length());
            return ApiResponse.success("获取成功", url);
        } catch (IllegalStateException e) {
            log.error("无法获取用户信息", e);
            return ApiResponse.error(500, "无法获取用户信息: " + e.getMessage());
        } catch (Exception e) {
            log.error("获取文件URL过程中发生异常", e);
            return ApiResponse.error(500, "获取文件URL失败: " + e.getMessage());
        }
    }

    /**
     * 验证并获取当前用户实体编码
     *
     * @return 用户实体编码
     * @throws IllegalStateException 如果无法获取用户信息
     */
    public String validateAndGetUserEntityCode() {
        // 使用现有的工具类生成用户实体编码
        String userEntityCode = UserEntityCodeUtil.getCurrentUserEntityCode();
        if (userEntityCode == null) {
            throw new IllegalStateException("无法获取当前用户信息");
        }
        return userEntityCode;
    }
    
    /**
     * 处理异常情况并返回错误信息
     *
     * @param response HttpServletResponse对象
     * @param message 错误信息
     */
    private void handleException(HttpServletResponse response, String message) {
        try {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            response.setContentType("text/plain");
            response.getWriter().write(message);
        } catch (IOException e) {
            log.error("处理异常时发生错误", e);
        }
    }
}