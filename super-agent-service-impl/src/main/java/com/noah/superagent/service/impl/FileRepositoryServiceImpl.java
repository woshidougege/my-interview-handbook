package com.noah.superagent.service.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.noah.superagent.common.config.KunlunProperties;
import com.noah.superagent.common.dto.response.FileUploadResponse;
import com.noah.superagent.service.FileRepositoryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.apache.commons.io.IOUtils;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletResponse;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * 文件仓库服务实现类
 *
 * @author 任相鹏
 * @since 1.0.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FileRepositoryServiceImpl implements FileRepositoryService {

    private final KunlunProperties kunlunProperties;

    @Override
    public FileUploadResponse uploadFile(String userEntityCode ,String directory, MultipartFile file) {
        OkHttpClient client = new OkHttpClient();
        
        try {
            // 构建请求URL - 使用配置项
            String uploadUrl = kunlunProperties.getFileRepository().getUploadPath();

            // 检查URL配置
            if (uploadUrl == null || uploadUrl.isEmpty()) {
                throw new IllegalStateException("文件上传URL未配置");
            }

            log.info("调用文件上传接口: {}", uploadUrl);

            // 构建params参数
            Map<String, String> paramsMap = new HashMap<>();
            paramsMap.put("entityCode", userEntityCode);
            paramsMap.put("directory", directory != null ? directory : "");

            ObjectMapper objectMapper = new ObjectMapper();
            String paramsJson = objectMapper.writeValueAsString(paramsMap);

            // 打印完整请求体用于调试
            log.info("发送到远程服务的请求体: params={}", paramsJson);

            // 构建multipart请求体
            RequestBody requestBody = new MultipartBody.Builder()
                    .setType(MultipartBody.FORM)
                    .addFormDataPart("params", null, RequestBody.create(paramsJson, MediaType.parse("application/json")))
                    .addFormDataPart("file", file.getOriginalFilename(), 
                            RequestBody.create(file.getBytes(), MediaType.parse("application/octet-stream")))
                    .build();

            // 创建请求
            Request request = new Request.Builder()
                    .url(uploadUrl)
                    .addHeader("Accept", "*/*")
                    .post(requestBody)
                    .build();

            // 执行请求
            try (Response response = client.newCall(request).execute()) {
                // 获取响应
                String responseString = response.body() != null ? response.body().string() : "";
                log.info("文件上传接口响应: {}", responseString);

                // 尝试解析JSON响应
                try {
                    Map<String, Object> responseMap = objectMapper.readValue(responseString, new TypeReference<Map<String, Object>>() {
                    });

                    // 检查响应码
                    Object codeObj = responseMap.get("code");
                    if (codeObj instanceof Number && ((Number) codeObj).intValue() == 100000) {
                        Object dataObj = responseMap.get("data");
                        if (dataObj instanceof Map) {
                            Map<String, Object> dataMap = (Map<String, Object>) dataObj;

                            FileUploadResponse fileUploadResponse = new FileUploadResponse();
                            fileUploadResponse.setName((String) dataMap.get("name"));
                            fileUploadResponse.setUrl((String) dataMap.get("url"));
                            return fileUploadResponse;
                        }
                    }

                    // 获取原始错误信息
                    String errorMessage = (String) responseMap.get("message");
                    if (errorMessage == null || errorMessage.isEmpty()) {
                        errorMessage = "文件上传接口调用失败";
                    }

                    log.warn("文件上传接口调用失败，返回码: {}，错误信息: {}", codeObj, errorMessage);
                    FileUploadResponse errorResponse = new FileUploadResponse();
                    errorResponse.setName("upload_failed");
                    errorResponse.setUrl(errorMessage); // 将原始错误信息传递回去
                    return errorResponse;
                } catch (Exception jsonException) {
                    // 如果不是有效的JSON响应，将原始响应内容作为错误信息返回
                    log.warn("文件上传接口返回非JSON格式响应: {}", responseString);
                    FileUploadResponse errorResponse = new FileUploadResponse();
                    errorResponse.setName("upload_failed");
                    errorResponse.setUrl(responseString); // 将原始响应内容传递给前端
                    return errorResponse;
                }
            }

        } catch (Exception e) {
            log.error("调用文件上传接口时发生异常", e);
            FileUploadResponse errorResponse = new FileUploadResponse();
            errorResponse.setName("upload_failed");
            errorResponse.setUrl("调用文件上传接口时发生异常: " + e.getMessage());
            return errorResponse;
        }
    }

    @Override
    public List<String> listObjectNames(String directory, boolean recursive, String userEntityCode) {
        try {
            // 构建请求URL - 使用配置项
            String url = kunlunProperties.getFileRepository().getListObjectNamesPath()
                            .replace("{userEntityCode}", userEntityCode)
                            .replace("{abilityCode}", kunlunProperties.getAbilityCodes().getDefaultCode());

            log.info("调用文件列表接口: {}", url);

            // 创建RestTemplate实例
            RestTemplate restTemplate = new RestTemplate();

            // 设置请求头
            HttpHeaders headers = new HttpHeaders();
            headers.set("Accept", "application/json");
            headers.set("Content-Type", "application/json");

            // 构建请求体
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("directory", directory != null ? directory : "");
            requestBody.put("recursive", recursive);

            // 创建请求实体
            org.springframework.http.HttpEntity<Map<String, Object>> entity = new org.springframework.http.HttpEntity<>(requestBody, headers);

            // 使用POST方法
            ResponseEntity<String> response = restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    entity,
                    String.class
            );

            log.info("文件列表接口响应: {}", response.getBody());

            // 尝试解析JSON响应
            try {
                // 解析响应
                ObjectMapper objectMapper = new ObjectMapper();
                Map<String, Object> responseMap = objectMapper.readValue(response.getBody(), new TypeReference<Map<String, Object>>() {
                });

                // 检查响应码
                Object codeObj = responseMap.get("code");
                if (codeObj instanceof Number && ((Number) codeObj).intValue() == 0) {
                    Object dataObj = responseMap.get("data");
                    if (dataObj instanceof Map) {
                        Map<String, Object> dataMap = (Map<String, Object>) dataObj;
                        Object dataArrayObj = dataMap.get("data");
                        if (dataArrayObj instanceof List) {
                            List<Object> dataArray = (List<Object>) dataArrayObj;
                            if (!dataArray.isEmpty() && dataArray.get(0) instanceof Map) {
                                Map<String, Object> firstItem = (Map<String, Object>) dataArray.get(0);
                                Object resultObj = firstItem.get("result");
                                if (resultObj instanceof String) {
                                    // 解析JSON字符串数组
                                    String resultStr = (String) resultObj;
                                    List<String> fileNames = objectMapper.readValue(resultStr, new TypeReference<List<String>>() {});
                                    return fileNames;
                                }
                            }
                        }
                    }
                }

                // 当返回码不是成功状态时，抛出异常
                String errorMsg = (String) responseMap.getOrDefault("msg", "文件列表接口调用失败");
                log.warn("文件列表接口调用失败，返回码: {}", codeObj);
                throw new RuntimeException("文件列表接口调用失败，错误码: " + codeObj + "，错误信息: " + errorMsg);
            } catch (Exception jsonException) {
                // 如果不是有效的JSON响应，将原始响应内容作为异常信息抛出
                log.warn("文件列表接口返回非JSON格式响应: {}", response.getBody());
                throw new RuntimeException("文件列表接口返回非JSON格式响应: " + response.getBody());
            }

        } catch (Exception e) {
            log.error("调用文件列表接口时发生异常", e);
            // 发生异常时重新抛出，让上层处理
            throw new RuntimeException("调用文件列表接口时发生异常: " + e.getMessage(), e);
        }
    }

    @Override
    public String getObjectURL(String filename, String userEntityCode) {
        try {
            // 构建请求URL - 使用配置项
            String url = kunlunProperties.getFileRepository().getGetObjectUrlPath()
                            .replace("{userEntityCode}", userEntityCode)
                            .replace("{abilityCode}", kunlunProperties.getAbilityCodes().getDefaultCode());

            log.info("调用获取文件URL接口: {}", url);
            
            // 创建RestTemplate实例
            RestTemplate restTemplate = new RestTemplate();
            
            // 设置请求头
            HttpHeaders headers = new HttpHeaders();
            headers.set("Accept", "application/json");
            headers.set("Content-Type", "application/json");
            
            // 构建请求体
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("filename", filename);
            
            // 创建请求实体
            org.springframework.http.HttpEntity<Map<String, Object>> entity = new org.springframework.http.HttpEntity<>(requestBody, headers);
            
            // 使用POST方法
            ResponseEntity<String> response = restTemplate.exchange(
                url, 
                HttpMethod.POST, 
                entity, 
                String.class
            );
            
            log.info("获取文件URL接口响应: {}", response.getBody());
            
            // 尝试解析JSON响应
            try {
                // 解析响应
                ObjectMapper objectMapper = new ObjectMapper();
                Map<String, Object> responseMap = objectMapper.readValue(response.getBody(), new TypeReference<Map<String, Object>>() {});
                
                // 检查响应码
                Object codeObj = responseMap.get("code");
                if (codeObj instanceof Number && ((Number) codeObj).intValue() == 0) { // 根据文档，成功码是0
                    // 成功响应
                    Object dataObj = responseMap.get("data");
                    if (dataObj instanceof Map) {
                        Map<String, Object> dataMap = (Map<String, Object>) dataObj;
                        Object dataArrayObj = dataMap.get("data");
                        if (dataArrayObj instanceof List) {
                            List<Object> dataArray = (List<Object>) dataArrayObj;
                            if (!dataArray.isEmpty() && dataArray.get(0) instanceof Map) {
                                Map<String, Object> firstItem = (Map<String, Object>) dataArray.get(0);
                                Object resultObj = firstItem.get("result");
                                if (resultObj instanceof String) {
                                    // 返回预签名URL
                                    return (String) resultObj;
                                }
                            }
                        }
                    }
                }
                
                // 当返回码不是成功状态时，抛出异常
                String errorMsg = (String) responseMap.getOrDefault("msg", "获取文件URL接口调用失败");
                log.warn("获取文件URL接口调用失败，返回码: {}", codeObj);
                throw new RuntimeException("获取文件URL接口调用失败，错误码: " + codeObj + "，错误信息: " + errorMsg);
            } catch (Exception jsonException) {
                // 如果不是有效的JSON响应，将原始响应内容作为异常信息抛出
                log.warn("获取文件URL接口返回非JSON格式响应: {}", response.getBody());
                throw new RuntimeException("获取文件URL接口返回非JSON格式响应: " + response.getBody());
            }
            
        } catch (Exception e) {
            log.error("调用获取文件URL接口时发生异常", e);
            // 发生异常时重新抛出，让上层处理
            throw new RuntimeException("调用获取文件URL接口时发生异常: " + e.getMessage(), e);
        }
    }
    
    /**
     * 构建目录路径
     * 
     * @param contextId 上下文ID
     * @param taskId 任务ID
     * @return 目录路径
     */
    @Override
    public String buildDirectoryPath(String contextId, String taskId) {
        StringBuilder directoryBuilder = new StringBuilder();
        
        if (contextId != null && !contextId.isEmpty()) {
            directoryBuilder.append(contextId);
            if (taskId != null && !taskId.isEmpty()) {
                directoryBuilder.append("/").append(taskId);
            }
        }
        
        return directoryBuilder.toString();
    }
    
    /**
     * 下载多个文件并打包成ZIP
     *
     * @param fileUris 文件URI列表
     * @param response HttpServletResponse对象
     * @throws IOException IO异常
     */
    @Override
    public void downloadMultipleFilesAsZip(List<String> fileUris, HttpServletResponse response) throws IOException {
        try (ZipOutputStream zipOut = new ZipOutputStream(response.getOutputStream())) {
            // 设置响应头
            response.setContentType("application/zip");
            response.setHeader("Content-Disposition", "attachment; filename=download.zip");

            // 下载并添加每个文件到ZIP
            for (String fileUri : fileUris) {
                try {
                    // 下载文件
                    URL url = new URL(fileUri);
                    try (InputStream in = url.openStream()) {
                        // 从URI中提取文件名
                        String fileName = extractFileNameFromUri(fileUri);
                        
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
                    log.error("处理文件时发生异常: fileUri={}", fileUri, e);
                    // 继续处理其他文件
                }
            }

            zipOut.finish();
            log.info("多文件打包下载完成: fileCount={}", fileUris.size());
        } catch (Exception e) {
            log.error("多文件打包下载过程中发生异常", e);
            throw e;
        }
    }
    
    /**
     * 下载单个文件
     *
     * @param fileUri  文件URI
     * @param response HttpServletResponse对象
     * @throws IOException IO异常
     */
    @Override
    public void downloadSingleFile(String fileUri, HttpServletResponse response) throws IOException {
        try {
            // 下载文件
            URL url = new URL(fileUri);
            try (InputStream in = url.openStream()) {
                // 从URI中提取文件名
                String fileName = extractFileNameFromUri(fileUri);
                
                // 设置响应头
                response.setContentType("application/octet-stream");
                response.setHeader("Content-Disposition", "attachment; filename=" + fileName);

                // 将文件内容写入响应
                IOUtils.copy(in, response.getOutputStream());
                response.flushBuffer();
            }
        } catch (Exception e) {
            log.error("下载单个文件时发生异常: fileUri={}", fileUri, e);
            throw e;
        }
    }
    
    /**
     * 从URI中提取文件名
     * 
     * @param uri 文件URI
     * @return 文件名
     */
    private String extractFileNameFromUri(String uri) {
        try {
            String path = new URL(uri).getPath();
            String fileName = path.substring(path.lastIndexOf('/') + 1);
            
            // 根据项目约定，取第一个下划线之后的部分作为文件名
            if (fileName.contains("_")) {
                fileName = fileName.substring(fileName.indexOf("_") + 1);
            }
            
            return fileName.isEmpty() ? "unknown_file" : fileName;
        } catch (Exception e) {
            log.warn("无法从URI中提取文件名: uri={}, 错误信息: {}", uri, e.getMessage());
            return "unknown_file";
        }
    }
}