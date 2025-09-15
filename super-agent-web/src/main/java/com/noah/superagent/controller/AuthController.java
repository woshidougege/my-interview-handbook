//package com.noah.superagent.controller;
//
//import com.noah.superagent.common.dto.request.PasswordLoginRequest;
//import com.noah.superagent.common.dto.request.PhoneLoginRequest;
//// import com.noah.superagent.common.dto.request.RegisterRequest; // SSO模式下无需注册
//import com.noah.superagent.common.dto.request.ResetPasswordRequest;
//import com.noah.superagent.common.dto.response.UserResponse;
//import com.noah.superagent.util.UserContext;
//import com.noah.superagent.convert.SSOUserWebConvert;
//import com.noah.superagent.model.SSOUserInfo;
//import com.noah.superagent.response.ApiResponse;
//import org.springframework.web.client.RestTemplate;
//import io.swagger.v3.oas.annotations.Operation;
//import io.swagger.v3.oas.annotations.Parameter;
//import io.swagger.v3.oas.annotations.media.Content;
//import io.swagger.v3.oas.annotations.media.ExampleObject;
//import io.swagger.v3.oas.annotations.media.Schema;
//import io.swagger.v3.oas.annotations.tags.Tag;
//import lombok.RequiredArgsConstructor;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.beans.factory.annotation.Value;
//import org.springframework.http.*;
//import org.springframework.web.bind.annotation.*;
//
//import javax.servlet.http.Cookie;
//import javax.servlet.http.HttpServletRequest;
//import javax.servlet.http.HttpServletResponse;
//import java.util.HashMap;
//import java.util.Map;
//
///**
// * 认证控制器
// * 集成SSO认证逻辑，处理用户认证相关接口
// *
// * @author 任相鹏
// * @since 1.0.0
// */
//@Slf4j
//@RestController
//@RequestMapping("/api/v1/auth")
//@RequiredArgsConstructor
//@Tag(name = "认证管理", description = "用户认证相关接口（方舟认证系统对接）")
//public class AuthController {
//
//    private final SSOUserWebConvert ssoUserWebConvert;
//    private final RestTemplate restTemplate;
//    // SSO相关逻辑直接在Controller中处理
//
//    @Value("${sso.server.url:http://192.168.1.65:10000/sso-server}")
//    private String ssoServerUrl;
//
//    @Value("${sso.servicecode:super_agent}")
//    private String serviceCode;
//
//    // ========== SSO工具方法 ==========
//
//    /**
//     * 从HTTP请求中获取当前用户信息
//     */
//    private SSOUserInfo getCurrentSSOUserFromRequest(HttpServletRequest request) {
//        String token = extractTokenFromRequest(request);
//        if (token == null) {
//            log.debug("未找到SSO Token");
//            return null;
//        }
//
//        return getSSOUserByToken(token);
//    }
//
//    /**
//     * 根据Token获取SSO用户信息
//     */
//    private SSOUserInfo getSSOUserByToken(String token) {
//        try {
//            String url = ssoServerUrl + "/api/user/current";
//
//            HttpHeaders headers = new HttpHeaders();
//            headers.set("Authorization", "Bearer " + token);
//            headers.setContentType(MediaType.APPLICATION_JSON);
//
//            HttpEntity<String> entity = new HttpEntity<>(headers);
//
//            ResponseEntity<Map> response = restTemplate.exchange(
//                    url, HttpMethod.GET, entity, Map.class
//            );
//
//            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
//                Map<String, Object> responseBody = response.getBody();
//                Object data = responseBody.get("data");
//                if (data != null) {
//                    return parseUserInfo(data);
//                }
//            }
//
//            return null;
//        } catch (Exception e) {
//            log.error("调用SSO API获取用户信息失败: {}", e.getMessage(), e);
//            return null;
//        }
//    }
//
//    /**
//     * 从请求中提取SSO Token
//     */
//    private String extractTokenFromRequest(HttpServletRequest request) {
//        // 1. 尝试从Header获取Token
//        String token = request.getHeader("Authorization");
//        if (token != null && token.startsWith("Bearer ")) {
//            return token.substring(7);
//        }
//
//        // 2. 尝试从Cookie获取Token
//        Cookie[] cookies = request.getCookies();
//        if (cookies != null) {
//            for (Cookie cookie : cookies) {
//                if ("SSO_TOKEN".equals(cookie.getName())) {
//                    return cookie.getValue();
//                }
//            }
//        }
//
//        // 3. 尝试从请求参数获取Token
//        token = request.getParameter("token");
//        if (token != null && !token.trim().isEmpty()) {
//            return token;
//        }
//
//        return null;
//    }
//
//    /**
//     * 解析用户信息
//     */
//    private SSOUserInfo parseUserInfo(Object data) {
//        try {
//            if (data instanceof Map) {
//                Map<String, Object> userMap = (Map<String, Object>) data;
//
//                SSOUserInfo userInfo = new SSOUserInfo();
//                userInfo.setUserId((Integer) userMap.get("userId"));
//                userInfo.setUsername((String) userMap.get("username"));
//                userInfo.setPhone((String) userMap.get("phone"));
//                userInfo.setEmail((String) userMap.get("email"));
//                userInfo.setRealName((String) userMap.get("realName"));
//                userInfo.setOrgId((String) userMap.get("orgId"));
//                userInfo.setOrgName((String) userMap.get("orgName"));
//                userInfo.setStatus((Integer) userMap.get("status"));
//
//                return userInfo;
//            }
//        } catch (Exception e) {
//            log.error("解析用户信息失败: {}", e.getMessage(), e);
//        }
//
//        return null;
//    }
//
//    // ========== 认证接口 ==========
//
//    @PostMapping("/password-login")
//    @Operation(summary = "密码登录",
//            description = "使用用户名/手机号和密码登录")
//    public ApiResponse<Map<String, String>> passwordLogin(@RequestBody PasswordLoginRequest request) {
//        log.info("密码登录请求 - 账号: {}", request.getAccount());
//
//        try {
//            String url = ssoServerUrl + "/agent/sso/passwordLogin";
//
//            Map<String, String> requestBody = new HashMap<>();
//            requestBody.put("servicecode", serviceCode);
//            requestBody.put("account", request.getAccount());
//            requestBody.put("password", request.getPassword());
//
//            HttpHeaders headers = new HttpHeaders();
//            headers.setContentType(MediaType.APPLICATION_JSON);
//            HttpEntity<Map<String, String>> entity = new HttpEntity<>(requestBody, headers);
//
//            ResponseEntity<Map> response = restTemplate.postForEntity(url, entity, Map.class);
//
//            if (response.getStatusCode() == HttpStatus.OK) {
//                Map<String, Object> responseBody = response.getBody();
//                if (responseBody != null && "200".equals(String.valueOf(responseBody.get("code")))) {
//                    Map<String, String> result = new HashMap<>();
//                    result.put("message", "登录成功");
//                    return ApiResponse.success("登录成功", result);
//                }
//            }
//
//            return ApiResponse.error("登录失败，请检查账号密码");
//
//        } catch (Exception e) {
//            log.error("密码登录异常: {}", e.getMessage(), e);
//            return ApiResponse.error("登录失败: " + e.getMessage());
//        }
//    }
//
//    @PostMapping("/phone-login")
//    @Operation(summary = "手机验证码登录",
//            description = "使用手机号和验证码登录")
//    public ApiResponse<Map<String, String>> phoneLogin(@RequestBody PhoneLoginRequest request) {
//        log.info("手机验证码登录请求 - 手机号: {}", request.getPhone());
//
//        try {
//            String url = ssoServerUrl + "/agent/sso/phoneLogin";
//
//            Map<String, String> requestBody = new HashMap<>();
//            requestBody.put("servicecode", serviceCode);
//            requestBody.put("phone", request.getPhone());
//            requestBody.put("phoneCode", request.getPhoneCode());
//
//            HttpHeaders headers = new HttpHeaders();
//            headers.setContentType(MediaType.APPLICATION_JSON);
//            HttpEntity<Map<String, String>> entity = new HttpEntity<>(requestBody, headers);
//
//            ResponseEntity<Map> response = restTemplate.postForEntity(url, entity, Map.class);
//
//            if (response.getStatusCode() == HttpStatus.OK) {
//                Map<String, Object> responseBody = response.getBody();
//                if (responseBody != null && "200".equals(String.valueOf(responseBody.get("code")))) {
//                    Map<String, String> result = new HashMap<>();
//                    result.put("message", "登录成功");
//                    return ApiResponse.success("登录成功", result);
//                }
//            }
//
//            return ApiResponse.error("登录失败，请检查手机号和验证码");
//
//        } catch (Exception e) {
//            log.error("手机验证码登录异常: {}", e.getMessage(), e);
//            return ApiResponse.error("登录失败: " + e.getMessage());
//        }
//    }
//
//    @PostMapping("/register")
//    @Operation(summary = "用户注册（已停用）",
//            description = "SSO模式下，用户注册请在认证中心进行")
//    public ApiResponse<Map<String, String>> register(@RequestBody Map<String, Object> request) {
//        log.info("用户尝试注册，但SSO模式下已停用本地注册");
//        return ApiResponse.error("SSO模式下，请前往认证中心注册");
//    }
//
//    @PostMapping("/send-sms")
//    @Operation(summary = "发送短信验证码",
//            description = "向指定手机号发送验证码")
//    public ApiResponse<Map<String, String>> sendSms(@RequestBody Map<String, String> request) {
//        String phone = request.get("phone");
//        log.info("发送短信验证码请求 - 手机号: {}", phone);
//
//        try {
//            String url = ssoServerUrl + "/agent/sso/sendSms";
//
//            Map<String, String> requestBody = new HashMap<>();
//            requestBody.put("servicecode", serviceCode);
//            requestBody.put("phone", phone);
//
//            HttpHeaders headers = new HttpHeaders();
//            headers.setContentType(MediaType.APPLICATION_JSON);
//            HttpEntity<Map<String, String>> entity = new HttpEntity<>(requestBody, headers);
//
//            ResponseEntity<Map> response = restTemplate.postForEntity(url, entity, Map.class);
//
//            if (response.getStatusCode() == HttpStatus.OK) {
//                Map<String, Object> responseBody = response.getBody();
//                if (responseBody != null && "200".equals(String.valueOf(responseBody.get("code")))) {
//                    Map<String, String> result = new HashMap<>();
//                    result.put("message", "验证码发送成功");
//                    return ApiResponse.success("验证码发送成功", result);
//                }
//            }
//
//            return ApiResponse.error("验证码发送失败");
//
//        } catch (Exception e) {
//            log.error("发送短信验证码异常: {}", e.getMessage(), e);
//            return ApiResponse.error("验证码发送失败: " + e.getMessage());
//        }
//    }
//
//    @GetMapping("/user/current")
//    @Operation(summary = "获取当前用户信息",
//            description = "获取当前登录用户的详细信息")
//    public ApiResponse<UserResponse> getCurrentUser(HttpServletRequest request) {
//        log.info("获取当前用户信息请求");
//
//        try {
//            // 从用户上下文获取当前用户信息
//            SSOUserInfo ssoUserInfo = UserContext.getCurrentUser();
//            if (ssoUserInfo == null) {
//                // 如果上下文中没有，尝试从SSO直接获取
//                ssoUserInfo = getCurrentSSOUserFromRequest(request);
//            }
//
//            if (ssoUserInfo == null) {
//                return ApiResponse.error("用户未登录");
//            }
//
//            UserResponse response = ssoUserWebConvert.toResponse(ssoUserInfo);
//            return ApiResponse.success("获取当前用户信息成功", response);
//
//        } catch (Exception e) {
//            log.error("获取当前用户信息失败: {}", e.getMessage(), e);
//            return ApiResponse.error("获取用户信息失败: " + e.getMessage());
//        }
//    }
//
//    @GetMapping("/public-key")
//    @Operation(summary = "获取加密公钥",
//            description = "获取SM2加密算法的公钥，用于敏感信息加密传输")
//    public ApiResponse<Map<String, String>> getPublicKey() {
//        log.info("获取公钥请求");
//
//        try {
//            String url = ssoServerUrl + "/agent/sso/getSysClientInfo";
//
//            Map<String, String> requestBody = new HashMap<>();
//            requestBody.put("servicecode", serviceCode);
//
//            HttpHeaders headers = new HttpHeaders();
//            headers.setContentType(MediaType.APPLICATION_JSON);
//            HttpEntity<Map<String, String>> entity = new HttpEntity<>(requestBody, headers);
//
//            ResponseEntity<Map> response = restTemplate.postForEntity(url, entity, Map.class);
//
//            if (response.getStatusCode() == HttpStatus.OK) {
//                Map<String, Object> responseBody = response.getBody();
//                if (responseBody != null && "200".equals(String.valueOf(responseBody.get("code")))) {
//                    Object data = responseBody.get("data");
//                    if (data instanceof Map) {
//                        Map<String, Object> dataMap = (Map<String, Object>) data;
//                        Map<String, String> result = new HashMap<>();
//                        result.put("publicKey", (String) dataMap.get("publicKey"));
//                        return ApiResponse.success("获取公钥成功", result);
//                    }
//                }
//            }
//
//            return ApiResponse.error("获取公钥失败");
//
//        } catch (Exception e) {
//            log.error("获取公钥异常: {}", e.getMessage(), e);
//            return ApiResponse.error("获取公钥失败: " + e.getMessage());
//        }
//    }
//
//    @PostMapping("/reset-password")
//    @Operation(summary = "重置密码",
//            description = "使用手机验证码重置用户密码")
//    public ApiResponse<Map<String, String>> resetPassword(@RequestBody ResetPasswordRequest request) {
//        log.info("重置密码请求 - 手机号: {}", request.getPhone());
//        // TODO: 实现重置密码逻辑
//        return ApiResponse.error("SSO模式下，请在认证中心重置密码");
//    }
//}