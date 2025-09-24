package com.noah.superagent.common.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.util.Map;

/**
 * A2A补充信息请求DTO
 */
@Data
@Schema(description = "A2A补充信息请求")
public  class A2ASupplementInfoRequest {
    @NotBlank(message = "用户ID不能为空")
    private String userId;

    @NotBlank(message = "任务ID不能为空")
    private String taskId;

    @NotBlank(message = "上下文ID不能为空")
    private String contextId;

    @NotNull(message = "表单数据不能为空")
    private Map<String, Object> formData;

    @NotBlank(message = "A2A协议代理实体编码不能为空")
    private String a2aProtocolProxyEntityCode;

    // Getters and Setters
    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getTaskId() {
        return taskId;
    }

    public void setTaskId(String taskId) {
        this.taskId = taskId;
    }

    public String getContextId() {
        return contextId;
    }

    public void setContextId(String contextId) {
        this.contextId = contextId;
    }

    public Map<String, Object> getFormData() {
        return formData;
    }

    public void setFormData(Map<String, Object> formData) {
        this.formData = formData;
    }

    public String getA2aProtocolProxyEntityCode() {
        return a2aProtocolProxyEntityCode;
    }

    public void setA2aProtocolProxyEntityCode(String a2aProtocolProxyEntityCode) {
        this.a2aProtocolProxyEntityCode = a2aProtocolProxyEntityCode;
    }
}