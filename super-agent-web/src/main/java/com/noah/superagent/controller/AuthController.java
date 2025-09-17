package com.noah.superagent.controller;

import cn.hutool.core.util.ObjUtil;
import com.noah.superagent.common.dto.request.PasswordLoginRequest;
import com.noah.superagent.common.dto.request.PhoneLoginRequest;
import com.noah.superagent.common.dto.request.RegisterRequest;
import com.noah.superagent.common.dto.request.ResetPasswordRequest;
import com.noah.superagent.response.ApiResponse;
import com.noah.superagent.service.UserCreditService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
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
import org.springframework.web.util.UriComponentsBuilder;
import java.util.HashMap;
import java.util.Map;

import static com.noah.superagent.util.UserContext.getCurrentUserId;

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

    private final RestTemplate restTemplate;

    @Value("${sa-token.sso.server-url:}")
    private String ssoServerUrl;

    @Value("${sa-token.sso.servicecode:super_agent}")
    private String serviceCode;

    @Value("${sa-token.sso.sm2-key:}")
    private String sm2Key;
    
    @Value("${registration.callback.url:}")
    private String callbackUrl;

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


    private Map<String, Object> getDataFromResponse(Map<String, Object> response) {
        if (MapUtil.isEmpty(response)) {
            return MapUtil.newHashMap();
        }
        Object data = response.get("data");
        return convertToStringObjectMap(data);
    }

    /**
     * 安全地将Object转换为Map<String, Object>
     */
    private Map<String, Object> convertToStringObjectMap(Object obj) {
        if (!(obj instanceof Map)) {
            return MapUtil.newHashMap();
        }
        
        Map<?, ?> rawMap = (Map<?, ?>) obj;
        Map<String, Object> result = MapUtil.newHashMap();
        
        // 安全地转换每个键值对
        for (Map.Entry<?, ?> entry : rawMap.entrySet()) {
            Object key = entry.getKey();
            Object value = entry.getValue();
            
            // 确保键是String类型
            if (key instanceof String) {
                result.put((String) key, value);
            } else if (key != null) {
                // 如果键不是String，转换为String
                result.put(key.toString(), value);
            }
        }
        
        return result;
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
            String url = UriComponentsBuilder.fromHttpUrl(ssoServerUrl).pathSegment("agent","sso","doLogin").toUriString();

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

                // 登录成功后，异步发放每日积分
                handleDailyCreditsOnLogin(ticket);

                Map<String, String> responseData = new HashMap<>();
                responseData.put("ticket", ticket);
                return ApiResponse.success("登录成功", responseData);
            } else {
                return ApiResponse.error(String.valueOf(result.get("msg")));
            }
        }

        return ApiResponse.error("登录失败，请稍后重试");
    }

    /**
     * 处理登录成功后的每日积分发放
     */
    private void handleDailyCreditsOnLogin(String ticket) {
        try {
            // 通过ticket获取用户ID
            Object userIdObj = ILoginFlow.checkTicket(ticket, "/sso/doLoginByTicket");
            if (userIdObj != null) {
                final long userId;
                if (userIdObj instanceof Number) {
                    userId = ((Number) userIdObj).longValue();
                } else {
                    userId = Long.parseLong(userIdObj.toString());
                }
                
                log.info("用户登录成功，开始发放每日积分 - userId: {}", userId);
                
                // 异步发放每日积分，避免影响登录接口性能
                new Thread(() -> {
                    try {
                        userCreditService.giveFreePlanDailyBonusOnLogin(userId);
                    } catch (Exception e) {
                        log.warn("发放每日积分失败 - userId: {}, 错误: {}", userId, e.getMessage());
                    }
                }).start();
            }
        } catch (Exception e) {
            log.warn("处理每日积分发放时发生错误: {}", e.getMessage());
            // 不影响登录流程
        }
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
            String url = UriComponentsBuilder.fromHttpUrl(ssoServerUrl).pathSegment("agent","sso","doLogin").toUriString();

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



    @Autowired
    private com.norinrd.interfaces.loginFlow.ILoginFlow ILoginFlow;

    @Autowired
    private UserCreditService userCreditService;

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
            String url = UriComponentsBuilder.fromHttpUrl(ssoServerUrl).pathSegment("agent","sso","userRegister").toUriString();
            
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
                    
                    // 注册成功后执行回调
                    executeRegistrationCallback(data, request.getPhone());
                    
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

    /**
     * 执行注册回调
     * @param userData 用户数据
     * @param phone 手机号
     */
    private void executeRegistrationCallback(Map<String, Object> userData, String phone) {
        try {
            Object userIdObj = this.ILoginFlow.checkTicket((String) userData.get("ticket"), "/sso/doLoginByTicket");

            if (userIdObj == null) {
                log.warn("用户数据中未包含用户ID，跳过回调");
                return;
            }
            
            long userId;
            if (userIdObj instanceof Number) {
                userId = ((Number) userIdObj).longValue();
            } else {
                userId = Long.parseLong(userIdObj.toString());
            }
            
            log.info("开始执行注册回调 - userId: {}, phoneNum: {}", userId, phone);
            
            // 构建回调URL
            String fullUrl = UriComponentsBuilder.fromHttpUrl(callbackUrl)
                    .queryParam("userld", userId)  // 注意：接口参数名是userld而非userId
                    .queryParam("phoneNum", phone)
                    .toUriString();
            
            log.debug("回调URL: {}", fullUrl);
            
            // 发送GET请求
            restTemplate.getForEntity(fullUrl, String.class);

            log.info("注册回调执行成功 - userId: {}, phoneNum: {}", userId, phone);
        } catch (Exception e) {
            log.error("注册回调执行失败 - phoneNum: {}, 错误: {}", phone, e.getMessage(), e);
            // 不抛出异常，避免影响主流程
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
            // 如果配置文件中sm2-key不为空，则直接返回配置的值
            if (StrUtil.isNotBlank(sm2Key)) {
                Map<String, Object> data = new HashMap<>();
                data.put("publicKey", sm2Key);
                return ApiResponse.success("获取公钥成功", data);
            }
            
            String url = UriComponentsBuilder.fromHttpUrl(ssoServerUrl).pathSegment("getSysClientInfo").toUriString();
            
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
            String url = UriComponentsBuilder.fromHttpUrl(ssoServerUrl).pathSegment(  "agent","sso","resetPassword").toUriString();

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
            return ApiResponse.error(e.getMessage());
        }
    }

    /**
     * 修改密码接口
     * 用于已登录用户修改自己的密码
     */
    @PostMapping("/change-password")
    @Operation(
            summary = "修改密码",
            description = "已登录用户修改自己的密码，对接SSO系统",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "修改密码请求参数",
                    required = true,
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    name = "修改密码示例",
                                    value = "{\n" +
                                            "  \"newPassword\": \"SM2加密后的新密码\"\n" +
                                            "}"
                            )
                    )
            )
    )
    public ApiResponse<String> changePassword(@RequestBody Map<String, String> request) {
        String newPassword = request.get("newPassword");
        
        if (StrUtil.isBlank(newPassword)) {
            return ApiResponse.error("新密码不能为空");
        }
        
        // 从用户上下文获取当前用户ID
        Long currentUserId = getCurrentUserId();
        if (ObjUtil.isNull(currentUserId)) {
            return ApiResponse.error("用户未登录或登录已过期");
        }
        
        log.info("修改密码请求，用户ID: {}", currentUserId);

        try {
            String url = UriComponentsBuilder.fromHttpUrl(ssoServerUrl)
                    .pathSegment("agent", "sso", "restPassword")
                    .toUriString();

            // 构建请求参数
            Map<String, String> requestBody = MapUtil.<String, String>builder()
                    .put("userId", currentUserId.toString())
                    .put("pwd", newPassword) // 前端已经SM2加密过的密码
                    .put("servicecode", serviceCode)
                    .build();

            HttpEntity<Map<String, String>> requestEntity = new HttpEntity<>(requestBody);
            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                    url, HttpMethod.POST, requestEntity, MAP_TYPE_REFERENCE);

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                Map<String, Object> result = response.getBody();
                if ("200".equals(String.valueOf(result.get("code")))) {
                    return ApiResponse.success("密码修改成功");
                } else {
                    return ApiResponse.error(String.valueOf(result.get("msg")));
                }
            }

            return ApiResponse.error("密码修改失败，请稍后重试");

        } catch (Exception e) {
            log.error("修改密码失败: {}", e.getMessage(), e);
            return ApiResponse.error("修改密码失败: " + e.getMessage());
        }
    }
}