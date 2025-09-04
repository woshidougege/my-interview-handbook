package com.noah.superagent.websocket;


import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

public class ChatWebSocketHandler extends TextWebSocketHandler {

    // 存储所有活跃的WebSocket会话
    private static final Map<String, WebSocketSession> sessions = new ConcurrentHashMap<>();

    // 用于JSON处理
    private final ObjectMapper objectMapper = new ObjectMapper();

    // 火山引擎大模型API配置
    private static final String API_KEY = "6f48543d-f0c5-4917-b834-f392e47cde9a";
    private static final String API_URL = "https://ark.cn-beijing.volces.com/api/v3/chat/completions";
    private static final String MODEL_NAME = "doubao-seed-1-6-250615";

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        sessions.put(session.getId(), session);
        System.out.println("WebSocket连接已建立: " + session.getId());

        // 发送欢迎消息
        session.sendMessage(new TextMessage("欢迎使用火山引擎大模型对话系统！请输入您的问题。"));
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        String payload = message.getPayload();
        System.out.println("收到消息: " + payload);

        // 异步处理大模型请求，避免阻塞WebSocket线程
        CompletableFuture.supplyAsync(() -> {
            try {
                return processMessage(payload, session);
            } catch (Exception e) {
                e.printStackTrace();
                return "处理消息时发生错误: " + e.getMessage();
            }
        }).thenAccept(response -> {
            try {
                // 流式输出处理完成后的最终消息
                if (!response.startsWith("STREAM:")) {
                    session.sendMessage(new TextMessage(response));
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        sessions.remove(session.getId());
        System.out.println("WebSocket连接已关闭: " + session.getId());
    }

    /**
     * 处理用户发送的消息，调用真实的大模型进行处理
     * @param message 用户发送的消息
     * @param session WebSocket会话
     * @return 模型响应结果
     */
    private String processMessage(String message, WebSocketSession session) {
        try {
            // 检查是否是摘要请求
            if (message.startsWith("摘要:") || message.startsWith("summary:")) {
                String contentToSummarize = message.substring(message.indexOf(":") + 1).trim();
                return "本地摘要结果: " + generateLocalSummary(contentToSummarize);
            }
            
            // 调用火山引擎大模型API并支持流式输出
            return callVolcEngineModel(message, session);
        } catch (Exception e) {
            e.printStackTrace();
            return "模型调用失败: " + e.getMessage();
        }
    }

    /**
     * 调用火山引擎大模型API
     * @param message 用户消息
     * @param session WebSocket会话
     * @return 模型响应
     */
    private String callVolcEngineModel(String message, WebSocketSession session) throws Exception {
        // 构建请求数据
        ObjectNode requestJson = objectMapper.createObjectNode();
        requestJson.put("model", MODEL_NAME);
        requestJson.put("stream", true); // 启用流式输出

        // 构建messages数组
        ArrayNode messagesArray = objectMapper.createArrayNode();

        // 构建用户消息对象
        ObjectNode userMessage = objectMapper.createObjectNode();
        userMessage.put("role", "user");

        // 构建content数组（支持多模态输入）
        ArrayNode contentArray = objectMapper.createArrayNode();

        // 添加文本内容
        ObjectNode textContent = objectMapper.createObjectNode();
        textContent.put("type", "text");
        textContent.put("text", message);
        contentArray.add(textContent);

        userMessage.set("content", contentArray);
        messagesArray.add(userMessage);

        requestJson.set("messages", messagesArray);

        // 创建HTTP连接
        URL url = new URL(API_URL);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("POST");
        connection.setRequestProperty("Content-Type", "application/json");
        connection.setRequestProperty("Authorization", "Bearer " + API_KEY);
        connection.setDoOutput(true);

        // 发送请求
        String requestBody = objectMapper.writeValueAsString(requestJson);
        try (OutputStream os = connection.getOutputStream()) {
            byte[] input = requestBody.getBytes(StandardCharsets.UTF_8);
            os.write(input, 0, input.length);
        }

        // 读取响应
        int responseCode = connection.getResponseCode();
        if (responseCode == HttpURLConnection.HTTP_OK) {
            BufferedReader reader = new BufferedReader(
                    new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8));
            
            StringBuilder fullResponse = new StringBuilder();
            String line;
            
            // 流式处理响应
            while ((line = reader.readLine()) != null) {
                if (line.startsWith("data:")) {
                    String jsonData = line.substring(5).trim();
                    if ("[DONE]".equals(jsonData)) {
                        break;
                    }
                    
                    try {
                        JsonNode responseJson = objectMapper.readTree(jsonData);
                        JsonNode choices = responseJson.get("choices");
                        if (choices != null && choices.isArray() && choices.size() > 0) {
                            JsonNode firstChoice = choices.get(0);
                            JsonNode delta = firstChoice.get("delta");
                            if (delta != null) {
                                JsonNode content = delta.get("content");
                                if (content != null && !content.asText().isEmpty()) {
                                    String contentText = content.asText();
                                    fullResponse.append(contentText);
                                    // 发送流式响应到前端
                                    session.sendMessage(new TextMessage("STREAM:" + contentText));
                                }
                            }
                        }
                    } catch (Exception e) {
                        // 解析单行JSON失败，继续处理下一行
                        continue;
                    }
                }
            }
            reader.close();
            
            return "STREAM_COMPLETE";
        } else {
            // 读取错误响应
            BufferedReader reader = new BufferedReader(
                    new InputStreamReader(connection.getErrorStream(), StandardCharsets.UTF_8));
            StringBuilder response = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                response.append(line);
            }
            reader.close();

            System.err.println("API调用失败，状态码: " + responseCode + ", 错误信息: " + response.toString());
            return "模型调用失败，状态码: " + responseCode;
        }
    }

    /**
     * 生成本地摘要 - 不需要联网的简单摘要算法
     * @param content 需要摘要的内容
     * @return 摘要结果
     */
    private String generateLocalSummary(String content) {
        if (content == null || content.isEmpty()) {
            return "内容为空，无法生成摘要";
        }
        
        // 简单的摘要算法：提取前3句和后3句作为摘要
        String[] sentences = content.split("[。！？.!?]+");
        
        if (sentences.length <= 6) {
            return content; // 如果句子少于等于6句，直接返回原文
        }
        
        StringBuilder summary = new StringBuilder();
        summary.append("摘要: ");
        
        // 添加前3句
        for (int i = 0; i < 3 && i < sentences.length; i++) {
            if (!sentences[i].trim().isEmpty()) {
                summary.append(sentences[i].trim()).append(" ");
            }
        }
        
        summary.append("... ");
        
        // 添加后3句
        int start = Math.max(sentences.length - 3, 3);
        for (int i = start; i < sentences.length; i++) {
            if (!sentences[i].trim().isEmpty()) {
                summary.append(sentences[i].trim()).append(" ");
            }
        }
        
        return summary.toString().trim();
    }

    /**
     * 向所有连接的客户端广播消息
     * @param message 要广播的消息
     */
    public static void broadcastMessage(String message) {
        sessions.values().forEach(session -> {
            try {
                if (session.isOpen()) {
                    session.sendMessage(new TextMessage(message));
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }
}
