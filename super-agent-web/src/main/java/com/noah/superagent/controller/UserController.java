package com.noah.superagent.controller;

import com.noah.superagent.common.base.BaseResponse;
import com.noah.superagent.common.dto.request.PageRequest;
import com.noah.superagent.common.dto.response.PageResponse;
import com.noah.superagent.common.dto.user.UserCreateRequest;
import com.noah.superagent.common.dto.user.UserUpdateRequest;
import com.noah.superagent.common.dto.user.UserResponse;
import com.noah.superagent.user.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;

/**
 * 用户管理控制器
 *
 * @author System
 * @since 1.0.0
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
@Validated
@Tag(name = "用户管理", description = "用户CRUD操作接口")
public class UserController {

    private final UserService userService;

    @PostMapping
    @Operation(summary = "创建用户", description = "注册新用户")
    public BaseResponse<UserResponse> createUser(@Valid @RequestBody UserCreateRequest request) {
        log.info("接收创建用户请求: {}", request.getPhone());
        
        try {
            UserResponse response = userService.createUser(request);
            return BaseResponse.success("用户创建成功", response);
        } catch (Exception e) {
            log.error("创建用户失败: {}", e.getMessage(), e);
            return BaseResponse.error(e.getMessage());
        }
    }

    @GetMapping("/{id}")
    @Operation(summary = "查询用户", description = "根据ID查询用户详情")
    public BaseResponse<UserResponse> getUserById(
            @Parameter(description = "用户ID", example = "1") 
            @PathVariable("id") Long id) {
        log.info("接收查询用户请求: {}", id);
        
        try {
            UserResponse response = userService.getUserById(id);
            return BaseResponse.success("查询成功", response);
        } catch (Exception e) {
            log.error("查询用户失败: {}", e.getMessage(), e);
            return BaseResponse.error(e.getMessage());
        }
    }

    @GetMapping
    @Operation(summary = "分页查询用户", description = "分页查询用户列表，支持关键词搜索")
    public BaseResponse<PageResponse<UserResponse>> getUserPage(@Valid PageRequest request) {
        log.info("接收分页查询用户请求: {}", request);
        
        try {
            PageResponse<UserResponse> response = userService.getUserPage(request);
            return BaseResponse.success("查询成功", response);
        } catch (Exception e) {
            log.error("分页查询用户失败: {}", e.getMessage(), e);
            return BaseResponse.error(e.getMessage());
        }
    }

    @PutMapping("/{id}")
    @Operation(summary = "更新用户", description = "更新用户信息")
    public BaseResponse<UserResponse> updateUser(
            @Parameter(description = "用户ID", example = "1") 
            @PathVariable("id") Long id,
            @Valid @RequestBody UserUpdateRequest request) {
        log.info("接收更新用户请求: {}", id);
        
        try {
            UserResponse response = userService.updateUser(id, request);
            return BaseResponse.success("更新成功", response);
        } catch (Exception e) {
            log.error("更新用户失败: {}", e.getMessage(), e);
            return BaseResponse.error(e.getMessage());
        }
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除用户", description = "根据ID删除用户")
    public BaseResponse<Void> deleteUser(
            @Parameter(description = "用户ID", example = "1") 
            @PathVariable("id") Long id) {
        log.info("接收删除用户请求: {}", id);
        
        try {
            userService.deleteUser(id);
            return BaseResponse.success();
        } catch (Exception e) {
            log.error("删除用户失败: {}", e.getMessage(), e);
            return BaseResponse.error(e.getMessage());
        }
    }

    @GetMapping("/check-phone")
    @Operation(summary = "检查手机号", description = "检查手机号是否已被注册")
    public BaseResponse<Boolean> checkPhone(
            @Parameter(description = "手机号", example = "13800138000") 
            @RequestParam("phone") String phone) {
        log.info("接收检查手机号请求: {}", phone);
        
        try {
            boolean exists = userService.isPhoneExists(phone);
            return BaseResponse.success("查询成功", exists);
        } catch (Exception e) {
            log.error("检查手机号失败: {}", e.getMessage(), e);
            return BaseResponse.error(e.getMessage());
        }
    }
}
