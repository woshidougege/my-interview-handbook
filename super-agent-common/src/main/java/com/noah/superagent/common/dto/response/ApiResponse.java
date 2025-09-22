package com.noah.superagent.common.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 统一API响应结果封装
 *
 * @param <T> 响应数据类型
 */
@Data
@Schema(description = "统一API响应结果封装")
public class ApiResponse<T> {
    
    @Schema(description = "响应码", example = "200")
    private int code;
    
    @Schema(description = "响应消息", example = "请求成功")
    private String message;
    
    @Schema(description = "响应数据")
    private T data;
    
    /**
     * 构造函数
     */
    public ApiResponse() {
    }
    
    /**
     * 构造函数
     *
     * @param code    响应码
     * @param message 响应消息
     * @param data    响应数据
     */
    public ApiResponse(int code, String message, T data) {
        this.code = code;
        this.message = message;
        this.data = data;
    }
    
    /**
     * 成功响应
     *
     * @param data 响应数据
     * @param <T>  数据类型
     * @return ApiResponse
     */
    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(200, "请求成功", data);
    }
    
    /**
     * 成功响应
     *
     * @param message 响应消息
     * @param data    响应数据
     * @param <T>     数据类型
     * @return ApiResponse
     */
    public static <T> ApiResponse<T> success(String message, T data) {
        return new ApiResponse<>(200, message, data);
    }
    
    /**
     * 失败响应
     *
     * @param code    响应码
     * @param message 响应消息
     * @param <T>     数据类型
     * @return ApiResponse
     */
    public static <T> ApiResponse<T> error(int code, String message) {
        return new ApiResponse<>(code, message, null);
    }
    
    /**
     * 失败响应
     *
     * @param message 响应消息
     * @param <T>     数据类型
     * @return ApiResponse
     */
    public static <T> ApiResponse<T> error(String message) {
        return new ApiResponse<>(500, message, null);
    }
}