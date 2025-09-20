package com.noah.superagent.common.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * A2A JSON-RPC请求DTO
 * 用于匹配标准的JSON-RPC 2.0请求格式
 */
@Data
@Schema(description = "A2A JSON-RPC请求")
public class A2AJsonRpcRequest {
    
    @Schema(description = "JSON-RPC版本", example = "2.0")
    private String jsonrpc = "2.0";
    
    @Schema(description = "方法名", example = "message/stream")
    private String method;
    
    @Schema(description = "请求ID", example = "requestId_09201034")
    private String id;
    
    @Schema(description = "请求参数")
    private A2ARequestParams params;
    
    /**
     * 请求参数内部类
     */
    @Data
    public static class A2ARequestParams {
        
        @Schema(description = "消息对象")
        private A2AMessage message;
        
        /**
         * 消息对象内部类
         */
        @Data
        public static class A2AMessage {
            
            @Schema(description = "角色", example = "user")
            private String role;
            
            @Schema(description = "消息部分")
            private A2AMessagePart[] parts;
            
            @Schema(description = "消息类型", example = "message")
            private String kind;
            
            @Schema(description = "任务ID", example = "Id_09201451")
            private String taskId;
            
            @Schema(description = "上下文ID", example = "Id_09201451")
            private String contextId;
        }
        
        /**
         * 消息部分内部类
         */
        @Data
        public static class A2AMessagePart {
            
            @Schema(description = "部分类型", example = "text")
            private String kind;
            
            @Schema(description = "文本内容", example = "写一个简单的HTML页面")
            private String text;
        }
    }
}