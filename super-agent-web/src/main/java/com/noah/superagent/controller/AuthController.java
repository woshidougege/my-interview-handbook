package com.noah.superagent.controller;

import com.noah.superagent.common.dto.response.UserResponse;
import com.noah.superagent.common.enums.UserStatusEnum;
import com.noah.superagent.convert.UserWebConvert;
import com.noah.superagent.model.UserDTO;
import com.noah.superagent.service.UserService;
import com.noah.superagent.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

/**
 * 认证相关控制器
 * 
 * 临时实现，后续会被单点登录替换
 *
 * @author Noah  
 * @since 1.0.0
 */
@Slf4j
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@Tag(name = "认证管理", description = "用户认证相关接口（临时实现）")
public class AuthController {

    private final UserService userService;
    private final UserWebConvert userWebConvert;

    @GetMapping("/user/current")
    @Operation(summary = "获取当前用户信息", 
            description = "获取当前登录用户的详细信息（临时实现，后续会被单点登录替换）")
    public ApiResponse<UserResponse> getCurrentUser() {
        log.info("获取当前用户信息请求");
        
        try {
            // 临时实现：返回第一个用户作为当前用户
            // TODO: 后续对接单点登录后，从SSO Token中获取用户信息
            UserDTO currentUser = getCurrentUserFromTemporaryLogic();
            
            UserResponse response = userWebConvert.toResponse(currentUser);
            return ApiResponse.success("获取当前用户信息成功", response);
            
        } catch (Exception e) {
            log.error("获取当前用户信息失败: {}", e.getMessage(), e);
            return ApiResponse.error("获取用户信息失败: " + e.getMessage());
        }
    }
    
    /**
     * 临时逻辑：获取当前用户
     * 
     * 这是一个临时实现，实际应该从以下来源获取：
     * 1. JWT Token
     * 2. Session
     * 3. SSO Token
     */
    private UserDTO getCurrentUserFromTemporaryLogic() {
        try {
            // 尝试获取数据库中的第一个用户
            var pageResponse = userService.getUserPage(1, 1, null);
            if (pageResponse != null && !pageResponse.getRecords().isEmpty()) {
                UserDTO firstUser = pageResponse.getRecords().get(0);
                log.info("使用临时逻辑返回用户: {} (ID: {})", firstUser.getNickname(), firstUser.getId());
                return firstUser;
            } else {
                // 如果数据库中没有用户，创建一个临时用户
                log.warn("数据库中没有用户，返回模拟用户数据");
                return createMockUser();
            }
        } catch (Exception e) {
            log.warn("获取数据库用户失败，返回模拟用户数据: {}", e.getMessage());
            return createMockUser();
        }
    }
    
    /**
     * 创建模拟用户数据
     */
    private UserDTO createMockUser() {
        UserDTO mockUser = new UserDTO();
        mockUser.setId(1001L);
        mockUser.setNickname("演示用户");
        mockUser.setPhone("13800138000");
        mockUser.setStatus(UserStatusEnum.ACTIVE);
        return mockUser;
    }
}
