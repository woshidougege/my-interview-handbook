package com.noah.superagent.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.noah.superagent.common.config.KunlunProperties;
import com.noah.superagent.common.dto.response.FileUploadResponse;
import com.noah.superagent.model.ChatFileItem;
import com.noah.superagent.model.SSOUserInfo;
import com.noah.superagent.response.ApiResponse;
import com.noah.superagent.service.FileRepositoryService;
import com.noah.superagent.util.UserEntityCodeUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static com.noah.superagent.util.UserContext.getCurrentUser;
import static com.noah.superagent.util.UserEntityCodeUtil.getCurrentUserAgentEntityCode;

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
    private final KunlunProperties kunlunProperties;

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
     * 根据文件URI列表下载文件
     * 如果只有一个文件，则直接下载该文件；如果有多个文件，则打包成ZIP下载
     * 可以选择是否压缩文件
     *
     * @param fileUris 文件URI列表
     * @param compress 是否压缩
     * @param response HttpServletResponse对象
     */
    @PostMapping("/downloadByUris")
    public void downloadByUris(
            @RequestBody List<String> fileUris,
            @RequestParam(value = "compress", defaultValue = "false") boolean compress,
            HttpServletResponse response) {
        log.info("接收到按文件URI下载请求: fileUris={}, compress={}", fileUris, compress);

        try {
            if (fileUris == null || fileUris.isEmpty()) {
                log.warn("文件URI列表为空");
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                response.getWriter().write("文件URI列表不能为空");
                return;
            }

            // 如果只有一个文件且不压缩，直接下载
            if (fileUris.size() == 1 && !compress) {
                fileRepositoryService.downloadSingleFile(fileUris.get(0), response);
            } else {
                // 如果有多个文件或需要压缩，打包下载
                fileRepositoryService.downloadMultipleFilesAsZip(fileUris, response);
            }
        } catch (Exception e) {
            log.error("按文件URI下载过程中发生异常", e);
            try {
                response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                response.getWriter().write("下载失败: " + e.getMessage());
            } catch (IOException ioException) {
                log.error("无法向响应写入错误信息", ioException);
            }
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
     * 从聊天历史记录中提取文件列表
     *
     * @param contextId 上下文ID
     * @return 文件列表
     */
    @GetMapping("/listChatFiles")
    public ApiResponse<List<ChatFileItem>> listChatFiles(
            @RequestParam(value = "contextId") String contextId) {
        
        log.info("接收到从聊天历史记录中提取文件列表请求: contextId={}", contextId);
        
        try {
            // 获取用户实体编码
            String entityCode = getCurrentUserAgentEntityCode();
            
            // 构建调用URL，使用配置文件中的URL
            String url = String.format("%s?sessionId=%s&entityCode=%s",
                    kunlunProperties.getChatHistory().getUrl(), contextId, entityCode);

            // 调用外部接口获取聊天历史详情
            RestTemplate restTemplate = new RestTemplate();
            ResponseEntity<String> historyResponse = restTemplate.getForEntity(url, String.class);

            log.info("聊天历史接口调用成功，状态码: {}", historyResponse.getStatusCode());

            // 从响应中提取文件信息
            List<ChatFileItem> fileList = new ArrayList<>();
            if (historyResponse.getStatusCode().is2xxSuccessful() && historyResponse.getBody() != null) {
                try {
                    ObjectMapper objectMapper = new ObjectMapper();
                    // 解析响应体
                    Map<String, Object> responseMap = objectMapper.readValue(historyResponse.getBody(),
                            new com.fasterxml.jackson.core.type.TypeReference<Map<String, Object>>() {
                            });
                    
                    Object dataObj = responseMap.get("data");
                    if (dataObj instanceof List) {
                        List<Map<String, Object>> dataList = (List<Map<String, Object>>) dataObj;
                        fileList = extractFilesFromChatHistory(dataList);
                    }
                } catch (Exception e) {
                    log.warn("解析聊天历史响应失败: ", e);
                    return ApiResponse.error(500, "解析聊天历史响应失败: " + e.getMessage());
                }
            }
            
            log.info("从聊天历史记录中提取文件列表成功: contextId={}, fileCount={}", contextId, fileList.size());
            return ApiResponse.success("获取成功", fileList);
        } catch (IllegalStateException e) {
            log.error("无法获取用户信息", e);
            return ApiResponse.error(500, "无法获取用户信息: " + e.getMessage());
        } catch (Exception e) {
            log.error("从聊天历史记录中提取文件列表过程中发生异常: contextId={}", contextId, e);
            return ApiResponse.error(500, "获取文件列表失败: " + e.getMessage());
        }
    }
    
    /**
     * 从聊天历史记录中提取文件列表
     *
     * @param chatHistory 聊天历史记录
     * @return 文件列表
     */
    @SuppressWarnings("unchecked")
    private List<ChatFileItem> extractFilesFromChatHistory(List<Map<String, Object>> chatHistory) {
        List<ChatFileItem> files = new ArrayList<>();
        
        for (Map<String, Object> item : chatHistory) {
            try {
                // 检查是否有artifact字段
                if (item.containsKey("artifact")) {
                    Map<String, Object> artifact = (Map<String, Object>) item.get("artifact");
                    
                    // 检查是否有parts字段
                    if (artifact.containsKey("parts")) {
                        List<Map<String, Object>> parts = (List<Map<String, Object>>) artifact.get("parts");
                        
                        // 遍历parts查找文件
                        for (Map<String, Object> part : parts) {
                            // 检查part是否为文件类型 (kind=file)
                            if ("file".equals(part.get("kind")) && part.containsKey("file")) {
                                Map<String, Object> fileData = (Map<String, Object>) part.get("file");
                                
                                ChatFileItem fileItem = new ChatFileItem();
                                fileItem.setName((String) fileData.get("name"));
                                fileItem.setUri((String) fileData.get("uri"));
                                
                                // 对于file类型，优先使用metadata.name作为displayName
                                if (part.containsKey("metadata")) {
                                    Map<String, Object> metadata = (Map<String, Object>) part.get("metadata");
                                    Object displayNameObj = metadata.get("name");
                                    if (displayNameObj instanceof String) {
                                        fileItem.setDisplayName((String) displayNameObj);
                                    } else {
                                        // 如果metadata中没有name，则从文件路径中提取
                                        String fullPath = (String) fileData.get("name");
                                        if (fullPath != null) {
                                            String[] pathParts = fullPath.split("/");
                                            fileItem.setDisplayName(pathParts[pathParts.length - 1]);
                                        }
                                    }
                                } else {
                                    // 如果没有metadata，则从文件路径中提取
                                    String fullPath = (String) fileData.get("name");
                                    if (fullPath != null) {
                                        String[] pathParts = fullPath.split("/");
                                        fileItem.setDisplayName(pathParts[pathParts.length - 1]);
                                    }
                                }
                                
                                // 从文件路径中提取简单文件名
                                String fullPath = (String) fileData.get("name");
                                if (fullPath != null) {
                                    String[] pathParts = fullPath.split("/");
                                    fileItem.setFileName(pathParts[pathParts.length - 1]);
                                }
                                
                                files.add(fileItem);
                            }
                            
                            // 检查part是否为数据类型 (kind=data) 并包含文件信息
                            if ("data".equals(part.get("kind")) && part.containsKey("data")) {
                                Map<String, Object> data = (Map<String, Object>) part.get("data");
                                
                                // 检查param是否为"选择文件"
                                Object paramObj = data.get("param");
                                if (paramObj instanceof String && "选择文件".equals(paramObj)) {
                                    // 检查是否包含value字段且看起来像URL
                                    Object valueObj = data.get("value");
                                    if (valueObj instanceof String) {
                                        String value = (String) valueObj;
                                        if (value.startsWith("http")) {
                                            ChatFileItem fileItem = new ChatFileItem();
                                            fileItem.setUri(value);
                                            
                                            // 设置文件名
                                            Object filenameObj = data.get("filename");
                                            if (filenameObj instanceof String) {
                                                fileItem.setFileName((String) filenameObj);
                                                fileItem.setName((String) filenameObj);
                                                // 对于data类型，使用filename作为displayName
                                                fileItem.setDisplayName((String) filenameObj);
                                            } else {
                                                // 从URL中提取文件名
                                                try {
                                                    String path = new URL(value).getPath();
                                                    String fileName = path.substring(path.lastIndexOf('/') + 1);
                                                    fileItem.setFileName(fileName);
                                                    fileItem.setName(fileName);
                                                    fileItem.setDisplayName(fileName);
                                                } catch (Exception e) {
                                                    fileItem.setFileName("unknown_file");
                                                    fileItem.setName("unknown_file");
                                                    fileItem.setDisplayName("unknown_file");
                                                }
                                            }
                                            
                                            files.add(fileItem);
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            } catch (Exception e) {
                log.warn("解析聊天历史记录中的文件信息时发生异常", e);
                // 继续处理其他记录
            }
        }
        
        return files;
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

}