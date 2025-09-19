package com.noah.superagent.controller;

import com.noah.superagent.common.dto.request.CreateOrderRequest;
import com.noah.superagent.common.dto.response.PaymentResponse;
import com.noah.superagent.common.dto.RefundRequest;
import com.noah.superagent.common.dto.RefundResponse;
import com.noah.superagent.common.dto.PaymentStatusEvent;
import com.noah.superagent.common.constants.PaymentStatus;
import com.noah.superagent.response.ApiResponse;
import com.noah.superagent.service.PaymentService;
import com.noah.superagent.sse.PaymentSseManager;
import com.noah.superagent.util.UserContext;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import javax.servlet.http.HttpServletRequest;
import com.github.binarywang.wxpay.bean.notify.SignatureHeader;
import javax.validation.Valid;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * 支付控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/payment")
@RequiredArgsConstructor
@Validated
@Tag(name = "支付管理", description = "支付相关接口")
public class PaymentController {

    private final PaymentService paymentService;
    private final PaymentSseManager paymentSseManager;

    @Operation(summary = "创建订单并发起支付", description = "创建订阅订单并发起微信支付")
    @PostMapping("/create")
    public ApiResponse<PaymentResponse> createOrderAndPay(
            @Valid @RequestBody CreateOrderRequest request) {
        
        // 自动获取当前登录用户ID
        Long userId = UserContext.requireCurrentUserId();
        PaymentResponse response = paymentService.createOrderAndPay(userId, request);
        return ApiResponse.success("创建订单成功", response);
    }

    @Operation(summary = "查询支付状态", description = "根据订单号查询支付状态")
    @GetMapping("/status/{orderNo}")
    public ApiResponse<PaymentResponse> queryPaymentStatus(
            @Parameter(description = "订单号") @PathVariable @NotBlank String orderNo) {
        
        PaymentResponse response = paymentService.queryPaymentStatus(orderNo);
        return ApiResponse.success("查询成功", response);
    }

    @Operation(summary = "获取用户订单列表", description = "获取指定用户的所有订单")
    @GetMapping("/orders/{userId}")
    public ApiResponse<List<PaymentResponse>> getUserOrders(
            @Parameter(description = "用户ID") @PathVariable @NotNull Long userId) {
        try {
            List<PaymentResponse> orders = paymentService.getUserOrders(userId);
            return ApiResponse.success("查询成功", orders);
        } catch (Exception e) {
            log.error("查询用户订单列表失败: userId={}, error={}", userId, e.getMessage(), e);
            return ApiResponse.error("查询订单列表失败: " + e.getMessage());
        }
    }

    @Operation(summary = "微信支付V3回调", description = "微信支付V3异步通知回调")
    @PostMapping("/wechat/notify")
    public String wechatPayNotify(@RequestBody String notifyData, HttpServletRequest request) {
        try {
            // 获取微信V3签名验证所需的请求头
            String signature = request.getHeader("Wechatpay-Signature");
            String timestamp = request.getHeader("Wechatpay-Timestamp");
            String nonce = request.getHeader("Wechatpay-Nonce");
            String serial = request.getHeader("Wechatpay-Serial");
            
            // 验证必要的头信息
            if (signature == null || timestamp == null || nonce == null || serial == null) {
                log.error("微信回调缺少必要的签名头信息");
                return "FAIL";
            }
            
            // 构造签名头对象
            SignatureHeader header = new SignatureHeader();
            header.setTimeStamp(timestamp);
            header.setNonce(nonce);
            header.setSerial(serial);
            header.setSignature(signature);
            
            // 处理微信支付回调
            boolean success = paymentService.handleWechatPayCallback(notifyData, header);
            
            if (success) {
                return "SUCCESS";
            } else {
                log.error("微信支付回调处理失败");
                return "FAIL";
            }
        } catch (Exception e) {
            log.error("微信支付回调处理异常: {}", e.getMessage(), e);
            return "FAIL";
        }
    }
    

    @Operation(summary = "取消订单", description = "取消未支付的订单")
    @PostMapping("/cancel/{orderNo}")
    public ApiResponse<Boolean> cancelOrder(
            @Parameter(description = "订单号") @PathVariable @NotBlank String orderNo) {
        
        boolean success = paymentService.cancelOrder(orderNo);
        return success ? ApiResponse.success("取消订单成功", true) 
                      : ApiResponse.error("取消订单失败");
    }

    @Operation(summary = "监听支付状态", description = "通过SSE实时监听支付状态变化")
    @GetMapping(value = "/status/listen/{orderNo}", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter listenPaymentStatus(
            @Parameter(description = "订单号") @PathVariable @NotBlank String orderNo) {
        
        // 创建SSE连接，设置超时时间为10分钟
        SseEmitter emitter = new SseEmitter(TimeUnit.MINUTES.toMillis(10));
        
        try {
            // 添加连接到管理器
            paymentSseManager.addConnection(orderNo, emitter);
            
            // 发送初始连接成功消息
            PaymentStatusEvent initEvent = PaymentStatusEvent.builder()
                    .orderNo(orderNo)
                    .status(PaymentStatus.LISTENING.getValue())
                    .message(PaymentStatus.LISTENING.getDescription())
                    .eventTime(LocalDateTime.now())
                    .build();
            
            try {
                // 使用与PaymentSseManager相同的序列化方式
                String eventData = paymentSseManager.getObjectMapper().writeValueAsString(initEvent);
                
                emitter.send(SseEmitter.event()
                        .name("connection")
                        .data(eventData));
            } catch (Exception e) {
                log.error("发送connection事件失败: orderNo={}", orderNo, e);
                throw e;
            }
            
            // 查询当前支付状态并推送
            try {
                PaymentResponse currentStatus = paymentService.queryPaymentStatus(orderNo);
                
                if (currentStatus != null && !PaymentStatus.PENDING.getValue().equals(currentStatus.getStatus()) 
                    && !PaymentStatus.WAITING.getValue().equals(currentStatus.getStatus())) {
                    
                    PaymentStatusEvent statusEvent = PaymentStatusEvent.builder()
                            .orderNo(orderNo)
                            .status(currentStatus.getStatus())
                            .amount(currentStatus.getAmount())
                            .paymentMethod(currentStatus.getPaymentMethod())
                            .message(PaymentStatus.getStatusDescription(currentStatus.getStatus()))
                            .eventTime(LocalDateTime.now())
                            .build();
                    
                    try {
                        String statusEventData = paymentSseManager.getObjectMapper().writeValueAsString(statusEvent);
                        
                        emitter.send(SseEmitter.event()
                                .name("payment_status")
                                .data(statusEventData));
                    } catch (Exception e) {
                        log.error("发送payment_status事件失败: orderNo={}", orderNo, e);
                    }
                }
            } catch (Exception e) {
                // 静默处理查询失败
            }
            
        } catch (Exception e) {
            log.error("建立SSE连接失败: orderNo={}", orderNo, e);
            emitter.completeWithError(e);
        }
        
        return emitter;
    }

    @Operation(summary = "获取支付监听状态", description = "获取当前支付监听的连接状态")
    @GetMapping("/listen/status/{orderNo}")
    public ApiResponse<Object> getListenStatus(
            @Parameter(description = "订单号") @PathVariable @NotBlank String orderNo) {
        
        int connectionCount = paymentSseManager.getConnectionCount(orderNo);
        int totalOrders = paymentSseManager.getActiveOrderCount();
        
        return ApiResponse.success("查询成功", Map.of(
                "orderNo", orderNo,
                "connectionCount", connectionCount,
                "totalActiveOrders", totalOrders,
                "isListening", connectionCount > 0
        ));
    }
    
    
    @Operation(summary = "申请退款", description = "对已支付的订单申请退款")
    @PostMapping("/refund")
    public ApiResponse<RefundResponse> applyRefund(@RequestBody @Valid RefundRequest request) {
        try {
            RefundResponse response = paymentService.applyRefund(request);
            return ApiResponse.success("退款申请提交成功", response);
        } catch (Exception e) {
            log.error("申请退款失败: orderNo={}, error={}", request.getOrderNo(), e.getMessage(), e);
            return ApiResponse.error("退款申请失败: " + e.getMessage());
        }
    }
    
    @Operation(summary = "查询退款状态", description = "查询订单的退款状态")
    @GetMapping("/refund/status/{orderNo}")
    public ApiResponse<RefundResponse> queryRefundStatus(
            @Parameter(description = "订单号") @PathVariable @NotBlank String orderNo) {
        try {
            RefundResponse response = paymentService.queryRefundStatus(orderNo);
            return ApiResponse.success("查询成功", response);
        } catch (Exception e) {
            log.error("查询退款状态失败: orderNo={}, error={}", orderNo, e.getMessage(), e);
            return ApiResponse.error("查询退款状态失败: " + e.getMessage());
        }
    }
    
    @Operation(summary = "测试退款（跳过状态检查）", 
               description = "仅用于测试的退款接口，跳过订单状态检查，可对任意状态的订单进行退款")
    @PostMapping("/test-refund")
    public ApiResponse<RefundResponse> testRefund(@RequestBody @Valid RefundRequest request) {
        try {
            // 在退款原因前添加测试标识
            if (request.getRefundReason() == null || !request.getRefundReason().startsWith("[测试退款]")) {
                request.setRefundReason("[测试退款] " + (request.getRefundReason() != null ? request.getRefundReason() : "测试退款"));
            }
            
            RefundResponse response = paymentService.testRefund(request);
            return ApiResponse.success("测试退款申请提交成功", response);
        } catch (Exception e) {
            log.error("测试退款失败: orderNo={}, error={}", request.getOrderNo(), e.getMessage(), e);
            return ApiResponse.error("测试退款失败: " + e.getMessage());
        }
    }
    
    @Operation(summary = "微信退款回调", description = "微信支付V3退款异步通知回调")
    @PostMapping("/wechat/refund/notify")
    public String wechatRefundNotify(@RequestBody String notifyData, HttpServletRequest request) {
        try {
            // 获取微信V3签名验证所需的请求头
            String signature = request.getHeader("Wechatpay-Signature");
            String timestamp = request.getHeader("Wechatpay-Timestamp");
            String nonce = request.getHeader("Wechatpay-Nonce");
            String serial = request.getHeader("Wechatpay-Serial");
            
            // 验证必要的头信息
            if (signature == null || timestamp == null || nonce == null || serial == null) {
                log.error("微信退款回调缺少必要的签名头信息");
                return "FAIL";
            }
            
            // 构造签名头对象
            SignatureHeader header = new SignatureHeader();
            header.setTimeStamp(timestamp);
            header.setNonce(nonce);
            header.setSerial(serial);
            header.setSignature(signature);
            
            // 处理微信退款回调
            boolean success = paymentService.handleWechatRefundCallback(notifyData, header);
            
            if (success) {
                return "SUCCESS";
            } else {
                log.error("微信退款回调处理失败");
                return "FAIL";
            }
        } catch (Exception e) {
            log.error("微信退款回调处理异常: {}", e.getMessage(), e);
            return "FAIL";
        }
    }
}
