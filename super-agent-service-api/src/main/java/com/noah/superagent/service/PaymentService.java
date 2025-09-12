package com.noah.superagent.service;

import com.noah.superagent.common.dto.request.CreateOrderRequest;
import com.noah.superagent.common.dto.response.PaymentResponse;
import com.noah.superagent.common.dto.RefundRequest;
import com.noah.superagent.common.dto.RefundResponse;
import com.github.binarywang.wxpay.bean.notify.SignatureHeader;

import java.util.List;

/**
 * 支付服务接口
 */
public interface PaymentService {

    /**
     * 创建订单并发起支付
     *
     * @param userId 用户ID
     * @param request 创建订单请求
     * @return 支付响应
     */
    PaymentResponse createOrderAndPay(Long userId, CreateOrderRequest request);

    /**
     * 查询支付状态
     *
     * @param orderNo 订单号
     * @return 支付响应
     */
    PaymentResponse queryPaymentStatus(String orderNo);
    
    /**
     * 获取用户订单列表
     *
     * @param userId 用户ID
     * @return 订单列表
     */
    List<PaymentResponse> getUserOrders(Long userId);

    /**
     * 处理微信支付回调
     *
     * @param callbackData 回调数据
     * @param header 签名头信息
     * @return 处理结果
     */
    boolean handleWechatPayCallback(String callbackData, SignatureHeader header);

    /**
     * 取消订单
     *
     * @param orderNo 订单号
     * @return 是否成功
     */
    boolean cancelOrder(String orderNo);
    
    /**
     * 申请退款
     *
     * @param request 退款请求
     * @return 退款响应
     */
    RefundResponse applyRefund(RefundRequest request);
    
    /**
     * 查询退款状态
     *
     * @param orderNo 订单号
     * @return 退款响应
     */
    RefundResponse queryRefundStatus(String orderNo);
    
    /**
     * 处理微信退款回调
     *
     * @param callbackData 回调数据
     * @param header 签名头信息
     * @return 处理结果
     */
    boolean handleWechatRefundCallback(String callbackData, SignatureHeader header);
}
