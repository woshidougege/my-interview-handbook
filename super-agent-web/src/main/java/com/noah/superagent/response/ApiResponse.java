package com.noah.superagent.response;

import com.noah.superagent.common.enums.ResponseCodeEnum;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 统一API响应结果封装 - 仅限Web模块使用
 *
 * @author 任相鹏
 * @since 1.0.0
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(
    name = "ApiResponse",
    title = "统一API响应结果",
    description = "系统统一的API响应格式，包含响应码、消息、数据和状态信息"
)
public class ApiResponse<T> {

    @Schema(description = "响应码")
    private Integer code;

    @Schema(description = "响应消息")
    private String message;

    @Schema(description = "响应数据")
    private T data;

    @Schema(description = "请求是否成功")
    private Boolean success;

    @Schema(description = "响应时间戳")
    private Long timestamp;

    public ApiResponse(Integer code, String message, T data) {
        this.code = code;
        this.message = message;
        this.data = data;
        this.success = code == 200;
        this.timestamp = System.currentTimeMillis();
    }

    // ========== 成功响应 ==========

    /**
     * 成功响应 - 带数据
     */
    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(200, "操作成功", data);
    }

    /**
     * 成功响应 - 无数据
     */
    public static <T> ApiResponse<T> success() {
        return new ApiResponse<>(200, "操作成功", null);
    }

    /**
     * 成功响应 - 自定义消息
     */
    public static <T> ApiResponse<T> success(String message, T data) {
        return new ApiResponse<>(200, message, data);
    }

    /**
     * 成功响应 - 自定义消息，无数据
     */
    public static <T> ApiResponse<T> success(String message) {
        return new ApiResponse<>(200, message, null);
    }

    // ========== 失败响应 ==========

    /**
     * 失败响应 - 使用枚举
     */
    public static <T> ApiResponse<T> error(ResponseCodeEnum responseCode) {
        return new ApiResponse<>(responseCode.getCode(), responseCode.getMessage(), null);
    }

    /**
     * 失败响应 - 自定义消息
     */
    public static <T> ApiResponse<T> error(String message) {
        return new ApiResponse<>(500, message, null);
    }

    /**
     * 失败响应 - 自定义错误码和消息
     */
    public static <T> ApiResponse<T> error(Integer code, String message) {
        return new ApiResponse<>(code, message, null);
    }

    /**
     * 失败响应 - 参数错误
     */
    public static <T> ApiResponse<T> badRequest(String message) {
        return new ApiResponse<>(400, message, null);
    }

    /**
     * 失败响应 - 未授权
     */
    public static <T> ApiResponse<T> unauthorized() {
        return new ApiResponse<>(401, "未授权", null);
    }

    /**
     * 失败响应 - 禁止访问
     */
    public static <T> ApiResponse<T> forbidden() {
        return new ApiResponse<>(403, "禁止访问", null);
    }

    /**
     * 失败响应 - 资源不存在
     */
    public static <T> ApiResponse<T> notFound(String message) {
        return new ApiResponse<>(404, message, null);
    }
}
