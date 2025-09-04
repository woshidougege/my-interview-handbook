package com.noah.superagent.dao.entity;

import com.mybatisflex.annotation.Column;
import com.mybatisflex.annotation.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Token使用记录表
 *
 * @author Noah
 * @since 1.0.0
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Table("t_token_usage_record")
public class TokenUsageRecordEntity extends BaseEntity {

    /**
     * 请求ID（幂等键）
     */
    @Column(value = "request_id")
    private String requestId;

    /**
     * 报告ID
     */
    @Column(value = "report_id")
    private String reportId;

    /**
     * 用户外部键
     */
    @Column(value = "user_external_key")
    private String userExternalKey;

    /**
     * 智能体ID
     */
    @Column(value = "agent_id")
    private String agentId;

    /**
     * 会话ID
     */
    @Column(value = "session_id")
    private String sessionId;

    /**
     * 模型名称
     */
    @Column(value = "model_name")
    private String modelName;

    /**
     * 输入Token数量
     */
    @Column(value = "input_tokens")
    private Long inputTokens;

    /**
     * 输出Token数量
     */
    @Column(value = "output_tokens")
    private Long outputTokens;


    /**
     * 请求描述
     */
    @Column(value = "description")
    private String description;
}
