package com.noah.superagent.service;

import com.noah.superagent.common.dto.request.CreateOrderRequest;
import com.noah.superagent.common.dto.response.PaymentResponse;
import com.noah.superagent.common.dto.RefundRequest;
import com.noah.superagent.common.dto.RefundResponse;
import com.noah.superagent.dao.entity.SubscriptionOrderEntity;
import com.github.binarywang.wxpay.bean.notify.SignatureHeader;
import com.github.binarywang.wxpay.bean.result.WxPayOrderQueryV3Result;

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
    
    /**
     * 批量同步处理中订单的状态
     * 
     * 查询所有处理中的订单（waiting, pending, refund_processing等），
     * 从微信API获取最新状态并更新本地数据库
     *
     * @return 同步的订单数量
     */
    int syncPendingOrdersStatus();
    
    /**
     * 检查并处理单个订单的支付超时
     * 
     * 针对特定订单进行超时检查：
     * 1. 如果订单已支付，返回false（无需处理）
     * 2. 如果订单未支付且未超时，主动查询微信状态
     * 3. 如果订单已超时，更新为超时状态
     *
     * @param orderNo 订单号
     * @return 是否更新了订单状态
     */
    boolean checkAndProcessPaymentTimeout(String orderNo);
    
    /**
     * 事务性更新订单相关的所有表
     * 
     * @param order 订单实体
     * @param newStatus 新状态
     * @param wxResult 微信查询结果（可为null）
     */
    void updateAllRelatedTablesWithTransaction(SubscriptionOrderEntity order, String newStatus, WxPayOrderQueryV3Result wxResult);
}
