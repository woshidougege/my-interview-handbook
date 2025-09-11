package com.noah.superagent.controller;

import com.noah.superagent.common.dto.request.PasswordLoginRequest;
import com.noah.superagent.common.dto.request.PhoneLoginRequest;
import com.noah.superagent.common.dto.request.RegisterRequest;
import com.noah.superagent.common.dto.request.ResetPasswordRequest;
import com.noah.superagent.common.dto.response.UserResponse;
import com.noah.superagent.convert.UserWebConvert;
import com.noah.superagent.model.UserDTO;
import com.noah.superagent.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;
import org.springframework.util.MultiValueMap;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.core.ParameterizedTypeReference;

import cn.hutool.core.util.StrUtil;
import cn.hutool.core.map.MapUtil;
import cn.hutool.core.util.URLUtil;
import cn.hutool.core.lang.Validator;

import java.util.HashMap;
import java.util.Map;

/**
 * 认证相关控制器 - 方舟认证系统对接
 *
 * @author 任相鹏  
 * @since 1.0.0
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Tag(name = "认证管理", description = "用户认证相关接口（方舟认证系统对接）")
public class AuthController {

    private final UserWebConvert userWebConvert;
    private final RestTemplate restTemplate;

    @Value("${sso.server.url:http://192.168.1.65:10000/sso-server}")
    private String ssoServerUrl;

    @Value("${sso.servicecode:super_agent}")
    private String serviceCode;

    /**
     * 创建 ParameterizedTypeReference 用于 Map<String, Object>
     */
    private static final ParameterizedTypeReference<Map<String, Object>> MAP_TYPE_REFERENCE =
            new ParameterizedTypeReference<>() {
            };

    /**
     * 验证手机号码格式 - 使用hutool工具库
     */
    private boolean isValidPhoneNumber(String phoneNumber) {
        if (StrUtil.isBlank(phoneNumber)) {
            return false;
        }
        // 移除所有空白字符
        phoneNumber = StrUtil.trim(phoneNumber);
        return Validator.isMobile(phoneNumber);
    }

    /**
     * 安全地构建URL，防止SSRF攻击 - 使用hutool工具库
     */
    private String buildSecureUrl(String baseUrl, Map<String, String> params) {
        String fullUrl = URLUtil.normalize(baseUrl + "/sms/send");
        if (MapUtil.isNotEmpty(params)) {
            StringBuilder urlBuilder = new StringBuilder(fullUrl);
            if (!fullUrl.contains("?")) {
                urlBuilder.append("?");
            } else {
                urlBuilder.append("&");
            }
            boolean first = true;
            for (Map.Entry<String, String> entry : params.entrySet()) {
                if (!first) {
                    urlBuilder.append("&");
                }
                urlBuilder.append(URLUtil.encode(entry.getKey())).append("=").append(URLUtil.encode(entry.getValue()));
                first = false;
            }
            return urlBuilder.toString();
        }
        return fullUrl;
    }

    /**
     * 安全地从Map中获取嵌套的Map数据 - 使用hutool工具库
     */
    @SuppressWarnings("unchecked")
    private Map<String, Object> getDataFromResponse(Map<String, Object> response) {
        if (MapUtil.isEmpty(response)) {
            return MapUtil.newHashMap();
        }
        Object data = response.get("data");
        if (data instanceof Map) {
            return (Map<String, Object>) data;
        }
        return MapUtil.newHashMap();
    }

    @PostMapping("/password-login")
    @Operation(
            summary = "密码登录",
            description = "使用用户名和SM2加密密码进行登录，对接方舟认证系统",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                description = "密码登录请求参数",
                required = true,
                content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = PasswordLoginRequest.class),
                    examples = @ExampleObject(
                        name = "密码登录示例",
                        value = "{\n" +
                                "  \"username\": \"admin\",\n" +
                                "  \"pwd\": \"304802...\",\n" +
                                "  \"servicecode\": \"super_agent\"\n" +
                                "}"
                    )
                )
            )
    )
    public ApiResponse<Map<String, String>> passwordLogin(@RequestBody PasswordLoginRequest loginRequest) {
        log.info("密码登录请求，用户名: {}", loginRequest.getUsername());

        try {
            String url = ssoServerUrl + "/agent/sso/doLogin";

            // 使用hutool的MapUtil构建请求参数，代码更简洁
            Map<String, String> requestBody = MapUtil.<String, String>builder()
                    .put("username", loginRequest.getUsername())
                    .put("pwd", loginRequest.getPwd())
                    .put("servicecode", serviceCode)
                    .build();

            return getMapApiResponse(url, requestBody);

        } catch (Exception e) {
            log.error("密码登录失败: {}", e.getMessage(), e);
            return ApiResponse.error("登录失败: " + e.getMessage());
        }
    }

    private ApiResponse<Map<String, String>> getMapApiResponse(String url, Map<String, String> requestBody) {
        HttpEntity<Map<String, String>> requestEntity = new HttpEntity<>(requestBody);
        ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                url, HttpMethod.POST, requestEntity, MAP_TYPE_REFERENCE);

        if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
            Map<String, Object> result = response.getBody();
            if ("200".equals(String.valueOf(result.get("code")))) {
                Map<String, Object> data = getDataFromResponse(result);
                String ticket = String.valueOf(data.get("ticket"));

                Map<String, String> responseData = new HashMap<>();
                responseData.put("ticket", ticket);
                return ApiResponse.success("登录成功", responseData);
            } else {
                return ApiResponse.error(String.valueOf(result.get("msg")));
            }
        }

        return ApiResponse.error("登录失败，请稍后重试");
    }

    @PostMapping("/phone-login")
    @Operation(
            summary = "手机验证码登录",
            description = "使用手机号和SM2加密验证码进行登录，对接方舟认证系统",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                description = "手机登录请求参数",
                required = true,
                content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = PhoneLoginRequest.class),
                    examples = @ExampleObject(
                        name = "手机登录示例",
                        value = "{\n" +
                                "  \"phone\": \"13800138000\",\n" +
                                "  \"phoneCode\": \"123456\",\n" +
                                "  \"servicecode\": \"super_agent\"\n" +
                                "}"
                    )
                )
            )
    )
    public ApiResponse<Map<String, String>> phoneLogin(@RequestBody PhoneLoginRequest loginRequest) {
        log.info("手机验证码登录请求，手机号: {}", loginRequest.getPhone());

        try {
            String url = ssoServerUrl + "/agent/sso/doLogin";

            // 使用hutool的MapUtil构建请求参数，代码更简洁
            Map<String, String> requestBody = MapUtil.<String, String>builder()
                    .put("phone", loginRequest.getPhone())
                    .put("phoneCode", loginRequest.getPhoneCode())
                    .put("servicecode", serviceCode)
                    .build();

            return getMapApiResponse(url, requestBody);

        } catch (Exception e) {
            log.error("手机验证码登录失败: {}", e.getMessage(), e);
            return ApiResponse.error("登录失败: " + e.getMessage());
        }
    }

    /**
     * 发送短信验证码接口
     */
    @PostMapping("/send-sms")
    @Operation(summary = "发送短信验证码", description = "发送短信验证码到指定手机号")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "200",
        description = "验证码发送成功",
        content = @Content(
            mediaType = "application/json",
            schema = @Schema(implementation = ApiResponse.class),
            examples = @ExampleObject(
                value = "{\n  \"code\": 200,\n  \"message\": \"验证码发送成功\"\n}"
            )
        )
    )
    public ApiResponse<String> sendSmsCode(
            @Parameter(description = "手机号码", required = true, example = "13800138000") 
            @RequestParam String phoneNumber) {
        log.info("发送短信验证码请求，手机号: {}", phoneNumber);

        // 安全验证：检查手机号格式，防止SSRF攻击
        if (!isValidPhoneNumber(phoneNumber)) {
            return ApiResponse.error("无效的手机号码格式");
        }

        try {
            // 使用安全的URL构建方法，防止SSRF攻击
            Map<String, String> params = MapUtil.of("phoneNumber", phoneNumber);
            String url = buildSecureUrl(ssoServerUrl, params);

            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                    url, HttpMethod.POST, null, MAP_TYPE_REFERENCE);

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                Map<String, Object> result = response.getBody();
                if ("200".equals(String.valueOf(result.get("code")))) {
                    return ApiResponse.success("验证码发送成功");
                } else {
                    return ApiResponse.error(String.valueOf(result.get("msg")));
                }
            }

            return ApiResponse.error("验证码发送失败，请稍后重试");

        } catch (Exception e) {
            log.error("发送短信验证码失败: {}", e.getMessage(), e);
            return ApiResponse.error("验证码发送失败: " + e.getMessage());
        }
    }

    /**
     * 用户注册接口
     */
    @PostMapping("/register")
    @Operation(
        summary = "用户注册",
        description = "新用户注册接口"
    )
    public ApiResponse<Map<String, String>> register(@RequestBody RegisterRequest request) {
        log.info("用户注册请求，用户名: {}, 手机号: {}", 
                request.getUsername(), request.getPhone());
        
        try {
            String url = ssoServerUrl + "/agent/sso/userRegister";
            
            Map<String, String> requestBody = new HashMap<>();
            requestBody.put("username", request.getUsername());
            requestBody.put("userPwd", request.getUserPwd());
            requestBody.put("phone", request.getPhone());
            requestBody.put("phoneCode", request.getPhoneCode());
            requestBody.put("servicecode", serviceCode);

            HttpEntity<Map<String, String>> requestEntity = new HttpEntity<>(requestBody);
            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                    url, HttpMethod.POST, requestEntity, MAP_TYPE_REFERENCE);
            
            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                Map<String, Object> result = response.getBody();
                if ("200".equals(String.valueOf(result.get("code")))) {
                    Map<String, Object> data = getDataFromResponse(result);
                    String ticket = String.valueOf(data.get("ticket"));
                    
                    Map<String, String> responseData = new HashMap<>();
                    responseData.put("ticket", ticket);
                    return ApiResponse.success("注册成功", responseData);
                } else {
                    return ApiResponse.error(String.valueOf(result.get("msg")));
                }
            }
            
            return ApiResponse.error("注册失败，请稍后重试");
            
        } catch (Exception e) {
            log.error("用户注册失败: {}", e.getMessage(), e);
            return ApiResponse.error("注册失败: " + e.getMessage());
        }
    }



    @GetMapping("/user/current")
    @Operation(summary = "获取当前用户信息", 
            description = "获取当前登录用户的详细信息")
    public ApiResponse<UserResponse> getCurrentUser() {
        log.info("获取当前用户信息请求");
        
        try {
            // 这里需要实现从SSO Token中获取用户信息的逻辑
            // 暂时返回模拟数据
            UserDTO userDTO = new UserDTO();
            userDTO.setId(1001L);
            userDTO.setUsername("演示用户");
            userDTO.setPhone("13800138000");
            
            UserResponse response = userWebConvert.toResponse(userDTO);
            return ApiResponse.success("获取当前用户信息成功", response);
            
        } catch (Exception e) {
            log.error("获取当前用户信息失败: {}", e.getMessage(), e);
            return ApiResponse.error("获取用户信息失败: " + e.getMessage());
        }
    }

    /**
     * 获取公钥信息接口
     * 对接方舟认证系统，获取SM2加密所需的公钥
     */
    @GetMapping("/public-key")
    @Operation(
        summary = "获取公钥信息", 
        description = "获取SM2加密所需的公钥信息，用于前端密码加密"
    )
    public ApiResponse<Map<String, Object>> getPublicKey() {
        log.info("获取公钥信息请求，serviceCode: {}", serviceCode);
        
        try {
            String url = ssoServerUrl + "/getSysClientInfo";
            
            // 使用POST请求，将serviceCode作为表单参数
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
            
            MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
            formData.add("serviceCode", serviceCode);
            
            HttpEntity<MultiValueMap<String, String>> requestEntity = new HttpEntity<>(formData, headers);
            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                    url, HttpMethod.POST, requestEntity, MAP_TYPE_REFERENCE);
            
            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                Map<String, Object> result = response.getBody();
                if ("200".equals(String.valueOf(result.get("code")))) {
                    Map<String, Object> data = getDataFromResponse(result);
                    return ApiResponse.success("获取公钥成功", data);
                } else {
                    return ApiResponse.error(String.valueOf(result.get("msg")));
                }
            }
            
            return ApiResponse.error("获取公钥失败，请稍后重试");
            
        } catch (Exception e) {
            log.error("获取公钥信息失败: {}", e.getMessage(), e);
            return ApiResponse.error("获取公钥失败: " + e.getMessage());
        }
    }

    @PostMapping("/reset-password")
    @Operation(
            summary = "找回密码",
            description = "通过手机号和短信验证码重置密码"
    )
    public ApiResponse<String> resetPassword(@RequestBody ResetPasswordRequest resetRequest) {
        log.info("找回密码请求，手机号: {}", resetRequest.getPhone());

        try {
            String url = ssoServerUrl + "/agent/sso/resetPassword";

            Map<String, String> requestBody = new HashMap<>();
            requestBody.put("phone", resetRequest.getPhone());
            requestBody.put("phoneCode", resetRequest.getPhoneCode());
            requestBody.put("newPassword", resetRequest.getNewPassword());
            requestBody.put("servicecode", serviceCode);

            HttpEntity<Map<String, String>> requestEntity = new HttpEntity<>(requestBody);
            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                    url, HttpMethod.POST, requestEntity, MAP_TYPE_REFERENCE);

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                Map<String, Object> result = response.getBody();
                if ("200".equals(String.valueOf(result.get("code")))) {
                    return ApiResponse.success("密码重置成功");
                } else {
                    return ApiResponse.error(String.valueOf(result.get("msg")));
                }
            }

            return ApiResponse.error("密码重置失败，请稍后重试");

        } catch (Exception e) {
            log.error("找回密码失败: {}", e.getMessage(), e);
            return ApiResponse.error("密码重置失败: " + e.getMessage());
        }
    }
}