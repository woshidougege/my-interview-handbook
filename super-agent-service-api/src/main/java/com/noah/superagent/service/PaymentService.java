package com.noah.superagent.service;

import com.noah.superagent.common.dto.request.CreateOrderRequest;
import com.noah.superagent.common.dto.response.PaymentResponse;

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
     * 处理微信支付回调
     *
     * @param callbackData 回调数据
     * @return 处理结果
     */
    boolean handleWechatPayCallback(String callbackData);

    /**
     * 处理支付宝支付回调
     *
     * @param callbackData 回调数据
     * @return 处理结果
     */
    boolean handleAlipayCallback(String callbackData);

    /**
     * 取消订单
     *
     * @param orderNo 订单号
     * @return 是否成功
     */
    boolean cancelOrder(String orderNo);
}
