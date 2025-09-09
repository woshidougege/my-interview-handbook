package com.noah.superagent.controller;

import com.noah.superagent.common.dto.request.CreateOrderRequest;
import com.noah.superagent.common.dto.response.PaymentResponse;
import com.noah.superagent.response.ApiResponse;
import com.noah.superagent.service.PaymentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import javax.validation.constraints.NotBlank;

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

    @Operation(summary = "创建订单并发起支付", description = "创建订阅订单并发起微信/支付宝支付")
    @PostMapping("/create")
    public ApiResponse<PaymentResponse> createOrderAndPay(
            @Parameter(description = "用户ID", example = "1") @RequestParam Long userId,
            @Valid @RequestBody CreateOrderRequest request) {
        
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

    @Operation(summary = "微信支付回调", description = "微信支付异步通知回调")
    @PostMapping("/wechat/notify")
    public String wechatPayNotify(@RequestBody String callbackData, HttpServletRequest request) {
        try {
            log.info("收到微信支付回调通知: {}", callbackData);
            
            // 获取请求头信息（用于验签）
            String signature = request.getHeader("Wechatpay-Signature");
            String timestamp = request.getHeader("Wechatpay-Timestamp");
            String nonce = request.getHeader("Wechatpay-Nonce");
            String serial = request.getHeader("Wechatpay-Serial");
            
            log.debug("微信支付回调头信息 - Signature: {}, Timestamp: {}, Nonce: {}, Serial: {}", 
                     signature, timestamp, nonce, serial);
            
            boolean success = paymentService.handleWechatPayCallback(callbackData);
            
            if (success) {
                return "<xml><return_code><![CDATA[SUCCESS]]></return_code><return_msg><![CDATA[OK]]></return_msg></xml>";
            } else {
                return "<xml><return_code><![CDATA[FAIL]]></return_code><return_msg><![CDATA[处理失败]]></return_msg></xml>";
            }
        } catch (Exception e) {
            log.error("微信支付回调处理异常", e);
            return "<xml><return_code><![CDATA[FAIL]]></return_code><return_msg><![CDATA[系统异常]]></return_msg></xml>";
        }
    }

    @Operation(summary = "支付宝支付回调", description = "支付宝支付异步通知回调")
    @PostMapping("/alipay/notify")
    public String alipayNotify(HttpServletRequest request) {
        try {
            // 获取所有请求参数（支付宝通过表单参数传递回调数据）
            StringBuilder callbackData = new StringBuilder();
            request.getParameterMap().forEach((key, values) -> {
                if (values != null && values.length > 0) {
                    callbackData.append(key).append("=").append(values[0]).append("&");
                }
            });
            
            String parameterString = callbackData.toString();
            if (parameterString.endsWith("&")) {
                parameterString = parameterString.substring(0, parameterString.length() - 1);
            }
            
            log.info("收到支付宝支付回调通知: {}", parameterString);
            
            boolean success = paymentService.handleAlipayCallback(parameterString);
            
            if (success) {
                return "success";
            } else {
                return "fail";
            }
        } catch (Exception e) {
            log.error("支付宝支付回调处理异常", e);
            return "fail";
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
}
