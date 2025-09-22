package com.noah.superagent.common.util;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.noah.superagent.common.dto.response.ChatHistoryItemResponse;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Slf4j
public class ChatHistoryParser {
    
    private static final ObjectMapper objectMapper = new ObjectMapper();
    
    /**
     * 解析聊天历史JSON字符串为ChatHistoryItemResponse列表
     * 
     * @param jsonStr 聊天历史JSON字符串
     * @return ChatHistoryItemResponse列表
     */
    public static List<ChatHistoryItemResponse> parseChatHistory(String jsonStr) {
        List<ChatHistoryItemResponse> chatHistoryList = new ArrayList<>();
        
        try {
            // 解析返回的JSON数据
            Map<String, Object> resultMap = objectMapper.readValue(jsonStr, 
                    new TypeReference<Map<String, Object>>() {});
            
            // 获取data字段
            List<Map<String, Object>> dataList = (List<Map<String, Object>>) resultMap.get("data");
            
            if (dataList == null) {
                return chatHistoryList;
            }
            
            // 转换为ChatHistoryItemResponse列表
            for (Map<String, Object> itemMap : dataList) {
                ChatHistoryItemResponse item = new ChatHistoryItemResponse();
                item.setContextId((String) itemMap.get("contextId"));
                item.setFinalResult((Boolean) itemMap.get("final"));
                item.setKind((String) itemMap.get("kind"));
                item.setTaskId((String) itemMap.get("taskId"));
                
                // 处理status字段
                Map<String, Object> statusMap = (Map<String, Object>) itemMap.get("status");
                if (statusMap != null) {
                    ChatHistoryItemResponse.StatusInfo status = new ChatHistoryItemResponse.StatusInfo();
                    status.setState((String) statusMap.get("state"));
                    status.setTimestamp((String) statusMap.get("timestamp"));
                    
                    // 处理message字段
                    Map<String, Object> messageMap = (Map<String, Object>) statusMap.get("message");
                    if (messageMap != null) {
                        ChatHistoryItemResponse.MessageInfo message = new ChatHistoryItemResponse.MessageInfo();
                        message.setContextId((String) messageMap.get("contextId"));
                        message.setKind((String) messageMap.get("kind"));
                        message.setMessageId((String) messageMap.get("messageId"));
                        message.setRole((String) messageMap.get("role"));
                        
                        // 处理parts字段 - 保持原始对象
                        List<Object> partsList = (List<Object>) messageMap.get("parts");
                        if (partsList != null) {
                            message.setParts(new ArrayList<>(partsList));
                        }
                        
                        status.setMessage(message);
                    }
                    
                    item.setStatus(status);
                }
                
                // 处理直接的message字段（用户消息）
                Map<String, Object> directMessageMap = (Map<String, Object>) itemMap.get("message");
                if (directMessageMap != null) {
                    ChatHistoryItemResponse.MessageInfo message = new ChatHistoryItemResponse.MessageInfo();
                    message.setContextId((String) directMessageMap.get("contextId"));
                    message.setKind((String) directMessageMap.get("kind"));
                    message.setMessageId((String) directMessageMap.get("messageId"));
                    message.setRole((String) directMessageMap.get("role"));
                    message.setTaskId((String) directMessageMap.get("taskId"));
                    
                    // 处理parts字段 - 保持原始对象
                    List<Object> partsList = (List<Object>) directMessageMap.get("parts");
                    if (partsList != null) {
                        message.setParts(new ArrayList<>(partsList));
                    }
                    
                    // 为这种类型的项创建一个简单的status
                    ChatHistoryItemResponse.StatusInfo status = new ChatHistoryItemResponse.StatusInfo();
                    status.setMessage(message);
                    item.setStatus(status);
                }
                
                chatHistoryList.add(item);
            }
        } catch (Exception e) {
            log.warn("解析聊天历史数据失败: ", e);
        }
        
        return chatHistoryList;
    }
}