package com.noah.superagent.service.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.noah.superagent.common.config.KunlunProperties;
import com.noah.superagent.common.dto.response.FileUploadResponse;
import com.noah.superagent.service.FileRepositoryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.entity.mime.content.StringBody;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
        CloseableHttpClient httpClient = null;
        CloseableHttpResponse response = null;

        try {
            // 构建请求URL - 使用配置项
            String uploadUrl = kunlunProperties.getFileRepository().getUploadPath();

            // 检查URL配置
            if (uploadUrl == null || uploadUrl.isEmpty()) {
                throw new IllegalStateException("文件上传URL未配置");
            }

            log.info("调用文件上传接口: {}", uploadUrl);

            // 创建HttpClient实例
            httpClient = HttpClients.createDefault();

            // 创建HttpPost请求
            HttpPost httpPost = new HttpPost(uploadUrl);

            // 设置请求头
            httpPost.setHeader("Accept", "*/*");


            // 构建params参数
            Map<String, String> paramsMap = new HashMap<>();
            paramsMap.put("entityCode", userEntityCode);
            paramsMap.put("directory", directory != null ? directory : "");

            ObjectMapper objectMapper = new ObjectMapper();
            String paramsJson = objectMapper.writeValueAsString(paramsMap);

            // 打印完整请求体用于调试
            log.info("发送到远程服务的请求体: params={}", paramsJson);

            // 构建multipart请求体
            HttpEntity multipartEntity = MultipartEntityBuilder.create()
                    .addPart("params", new StringBody(paramsJson, ContentType.APPLICATION_JSON))
                    .addBinaryBody("file", file.getInputStream(), ContentType.APPLICATION_OCTET_STREAM, file.getOriginalFilename())
                    .build();

            // 设置请求体
            httpPost.setEntity(multipartEntity);

            // 执行请求
            response = httpClient.execute(httpPost);

            // 获取响应
            String responseString = EntityUtils.toString(response.getEntity());
            log.info("文件上传接口响应: {}", responseString);

            // 解析响应
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

        } catch (Exception e) {
            log.error("调用文件上传接口时发生异常", e);
            FileUploadResponse errorResponse = new FileUploadResponse();
            errorResponse.setName("upload_failed");
            errorResponse.setUrl("调用文件上传接口时发生异常: " + e.getMessage());
            return errorResponse;
        } finally {
            // 关闭资源
            try {
                if (response != null) {
                    response.close();
                }
                if (httpClient != null) {
                    httpClient.close();
                }
            } catch (IOException e) {
                log.error("关闭HttpClient资源时发生异常", e);
            }
        }
    }

    @Override
    public List<String> listObjectNames( String directory, boolean recursive) {
        try {
            // 构建请求URL - 使用配置项
            String url = kunlunProperties.getFileRepository().getListObjectNamesPath()
                            .replace("{entityCode}", kunlunProperties.getEntityCodes().getDefaultCode())
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
                                String resultStr = (String) resultObj;
                                List<String> fileNames = objectMapper.readValue(resultStr, new TypeReference<List<String>>() {
                                });
                                return fileNames;
                            }
                        }
                    }
                }
            }

            log.warn("文件列表接口调用失败，返回码: {}", codeObj);
            return new ArrayList<>();

        } catch (Exception e) {
            log.error("调用文件列表接口时发生异常", e);
            return new ArrayList<>();
        }
    }

    @Override
    public String getObjectURL(String filename) {
        try {
            // 构建请求URL - 使用配置项
            String url =kunlunProperties.getFileRepository().getGetObjectUrlPath()
                            .replace("{entityCode}", kunlunProperties.getEntityCodes().getDefaultCode())
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
                                return (String) resultObj;
                            }
                        }
                    }
                }
            }

            log.warn("获取文件URL接口调用失败，返回码: {}", codeObj);
            return "";

        } catch (Exception e) {
            log.error("调用获取文件URL接口时发生异常", e);
            return "";
        }
    }

    // 新增方法：校验entityCode格式
    private boolean isValidEntityCode(String entityCode) {
        // 示例：检查是否为32位UUID格式（可按实际需求调整）
        if (entityCode == null || entityCode.length() < 8) {
            return false;
        }

        // 检查是否以ENTITY_开头
        if (!entityCode.startsWith("ENTITY_")) {
            log.warn("entityCode必须以ENTITY_开头: {}", entityCode);
            return false;
        }

        // 获取实体编码部分（去掉ENTITY_前缀）
        String entityPart = entityCode.substring(7);

        // 检查实体编码部分是否为空或仅包含空格
        if (entityPart == null || entityPart.trim().isEmpty()) {
            log.warn("entityCode的实体编码部分不能为空: {}", entityCode);
            return false;
        }

        // 放宽校验规则：允许字母、数字、下划线、连字符等常见字符
        if (!entityPart.matches("[a-zA-Z0-9_-]+")) {
            log.warn("entityCode的实体编码部分只能包含字母、数字、下划线和连字符: {}", entityCode);
            return false;
        }

        // 验证长度限制（例如最大长度为100）
        if (entityPart.length() > 100) {
            log.warn("entityCode的实体编码部分长度不能超过100: {}", entityCode);
            return false;
        }

        return true;
    }
}