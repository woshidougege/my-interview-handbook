package com.noah.superagent.common.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/**
 * 对话标题生成响应
 *
 * @author AI Assistant
 * @since 1.0.0
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@Schema(description = "对话标题生成响应")
public class ChatTitleGenerateResponse extends BaseResponse {

    @Schema(description = "生成的标题", example = "Java编程学习指南")
    private String title;

    @Schema(description = "是否为异步生成", example = "false")
    private Boolean async;

    @Schema(description = "生成耗时（毫秒）", example = "1500")
    private Long duration;

    /**
     * 创建同步响应
     */
    public static ChatTitleGenerateResponse createSyncResponse(String title, Long duration) {
        return new ChatTitleGenerateResponse(title, false, duration);
    }

    /**
     * 创建异步响应
     */
    public static ChatTitleGenerateResponse createAsyncResponse() {
        return new ChatTitleGenerateResponse(null, true, null);
    }
}
