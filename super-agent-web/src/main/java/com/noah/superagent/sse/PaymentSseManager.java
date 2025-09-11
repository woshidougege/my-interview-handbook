package com.noah.superagent.sse;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.noah.superagent.common.dto.PaymentStatusEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;

/**
 * 支付SSE连接管理器
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentSseManager {
    
    private final ObjectMapper objectMapper;
    
    /**
     * 存储所有SSE连接：orderNo -> SseEmitter集合
     * 一个订单可能有多个客户端监听（如用户打开多个浏览器标签页）
     */
    private final Map<String, CopyOnWriteArraySet<SseEmitter>> connections = new ConcurrentHashMap<>();
    
    /**
     * 添加SSE连接
     * 
     * @param orderNo 订单号
     * @param emitter SSE发射器
     */
    public void addConnection(String orderNo, SseEmitter emitter) {
        connections.computeIfAbsent(orderNo, k -> new CopyOnWriteArraySet<>()).add(emitter);
        
        // 设置连接断开时的清理逻辑
        emitter.onCompletion(() -> removeConnection(orderNo, emitter));
        emitter.onTimeout(() -> removeConnection(orderNo, emitter));
        emitter.onError(throwable -> {
            removeConnection(orderNo, emitter);
        });
    }
    
    /**
     * 移除SSE连接
     * 
     * @param orderNo 订单号
     * @param emitter SSE发射器
     */
    public void removeConnection(String orderNo, SseEmitter emitter) {
        CopyOnWriteArraySet<SseEmitter> emitters = connections.get(orderNo);
        if (emitters != null) {
            emitters.remove(emitter);
            if (emitters.isEmpty()) {
                connections.remove(orderNo);
            }
        }
    }
    
    /**
     * 向指定订单的所有连接推送支付状态事件
     * 
     * @param orderNo 订单号
     * @param event 支付状态事件
     */
    public void sendPaymentStatusEvent(String orderNo, PaymentStatusEvent event) {
        CopyOnWriteArraySet<SseEmitter> emitters = connections.get(orderNo);
        if (emitters == null || emitters.isEmpty()) {
            return;
        }
        
        try {
            String eventData = objectMapper.writeValueAsString(event);
            
            // 向所有连接推送事件
            emitters.removeIf(emitter -> {
                try {
                    emitter.send(SseEmitter.event()
                            .name("payment_status")
                            .data(eventData));
                    return false; // 发送成功，保留连接
                } catch (IOException e) {
                    return true; // 发送失败，移除连接
                }
            });
            
        } catch (Exception e) {
            log.error("序列化支付状态事件失败: orderNo={}", orderNo, e);
        }
    }
    
    /**
     * 获取活跃连接数
     * 
     * @param orderNo 订单号
     * @return 连接数
     */
    public int getConnectionCount(String orderNo) {
        CopyOnWriteArraySet<SseEmitter> emitters = connections.get(orderNo);
        return emitters != null ? emitters.size() : 0;
    }
    
    /**
     * 获取所有活跃订单数
     * 
     * @return 订单数
     */
    public int getActiveOrderCount() {
        return connections.size();
    }
    
    /**
     * 监听支付状态事件并推送给SSE客户端
     */
    @EventListener
    public void handlePaymentStatusEvent(PaymentStatusEvent event) {
        log.debug("收到支付状态事件: orderNo={}, status={}", event.getOrderNo(), event.getStatus());
        sendPaymentStatusEvent(event.getOrderNo(), event);
    }
    
    /**
     * 获取ObjectMapper，用于统一序列化
     */
    public ObjectMapper getObjectMapper() {
        return objectMapper;
    }
}
