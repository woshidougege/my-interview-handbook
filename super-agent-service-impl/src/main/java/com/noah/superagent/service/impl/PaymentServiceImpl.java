package com.noah.superagent.service.impl;

import com.mybatisflex.core.query.QueryWrapper;
import com.noah.superagent.common.dto.request.CreateOrderRequest;
import com.noah.superagent.common.dto.response.PaymentResponse;
import com.noah.superagent.common.dto.RefundRequest;
import com.noah.superagent.common.dto.RefundResponse;
import com.noah.superagent.common.dto.PaymentStatusEvent;
import com.noah.superagent.common.constants.PaymentStatus;
import org.springframework.context.ApplicationEventPublisher;
import com.noah.superagent.dao.entity.PaymentRecordEntity;
import com.noah.superagent.dao.entity.SubscriptionOrderEntity;
import com.noah.superagent.dao.entity.SubscriptionPlanEntity;
import com.noah.superagent.dao.entity.UserSubscriptionEntity;
import com.noah.superagent.dao.mapper.PaymentRecordMapper;
import com.noah.superagent.dao.mapper.SubscriptionOrderMapper;
import com.noah.superagent.dao.mapper.SubscriptionPlanMapper;
import com.noah.superagent.dao.mapper.UserSubscriptionMapper;
import com.noah.superagent.service.PaymentService;
import com.noah.superagent.service.UserCreditService;
import com.noah.superagent.common.enums.SubscriptionStatusEnum;
import com.github.binarywang.wxpay.bean.request.WxPayUnifiedOrderV3Request;
import com.github.binarywang.wxpay.bean.request.WxPayOrderQueryV3Request;
import com.github.binarywang.wxpay.bean.result.enums.TradeTypeEnum;
import com.github.binarywang.wxpay.bean.result.WxPayOrderQueryV3Result;
import com.github.binarywang.wxpay.bean.notify.WxPayNotifyV3Result;
import com.github.binarywang.wxpay.bean.notify.WxPayNotifyV3Result.DecryptNotifyResult;
import com.github.binarywang.wxpay.bean.notify.WxPayRefundNotifyV3Result;
import com.github.binarywang.wxpay.bean.notify.SignatureHeader;
import com.github.binarywang.wxpay.bean.request.WxPayRefundV3Request;
import com.github.binarywang.wxpay.bean.result.WxPayRefundV3Result;
import com.github.binarywang.wxpay.exception.WxPayException;
import com.github.binarywang.wxpay.service.WxPayService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;


/**
 * 支付服务实现 - 使用真实的微信支付V3 API
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentServiceImpl implements PaymentService {

    private final SubscriptionOrderMapper subscriptionOrderMapper;
    private final PaymentRecordMapper paymentRecordMapper;
    private final SubscriptionPlanMapper subscriptionPlanMapper;
    private final UserCreditService userCreditService;
    private final UserSubscriptionMapper userSubscriptionMapper;
    private final WxPayService wxPayService;
    
    // 使用ApplicationEventPublisher发布支付状态事件，避免循环依赖
    @Autowired
    private ApplicationEventPublisher eventPublisher;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public PaymentResponse createOrderAndPay(Long userId, CreateOrderRequest request) {
        try {
            // 1. 验证支付方式 - 只支持微信支付
            if (!"wechat".equals(request.getPaymentMethod())) {
                throw new RuntimeException("当前只支持微信支付");
            }

            // 2. 查询套餐信息
            SubscriptionPlanEntity plan = subscriptionPlanMapper.selectOneById(request.getPlanId());
            if (plan == null) {
                throw new RuntimeException("套餐不存在");
            }

            // 3. 计算订单金额
            BigDecimal amount = "monthly".equals(request.getBillingCycle()) 
                ? plan.getMonthlyPrice() 
                : plan.getYearlyPrice();

            // 4. 创建订单
            String orderNo = generateOrderNo();
            SubscriptionOrderEntity order = new SubscriptionOrderEntity();
            order.setOrderNo(orderNo);
            order.setUserId(userId);
            order.setPlanId(request.getPlanId());
            order.setPlanName(plan.getPlanName());
            order.setAmount(amount);
            order.setBillingCycle(request.getBillingCycle());
            order.setStatus(PaymentStatus.WAITING.getValue());
            order.setPaymentMethod("wechat");
            order.setExpiredAt(LocalDateTime.now().plusMinutes(15)); // 15分钟后过期
            
            subscriptionOrderMapper.insert(order);

            // 5. 创建微信支付
            PaymentResponse response = createWeChatPayV3(order);
            response.setOrderId(order.getId());
            response.setOrderNo(orderNo);
            response.setAmount(amount);
            response.setPaymentMethod("wechat");
            response.setStatus(PaymentStatus.WAITING.getValue());
            response.setExpiredAt(order.getExpiredAt());
            response.setCreatedAt(order.getCreateTime());

            return response;

        } catch (Exception e) {
            log.error("创建订单并发起支付失败: userId={}, request={}", userId, request, e);
            throw new RuntimeException("创建支付失败: " + e.getMessage());
        }
    }

    @Override
    public PaymentResponse queryPaymentStatus(String orderNo) {
        SubscriptionOrderEntity order = subscriptionOrderMapper.selectOneByQuery(
            QueryWrapper.create().where("order_no = ?", orderNo)
        );

        if (order == null) {
            throw new RuntimeException("订单不存在");
        }

        // 如果订单还在进行中，主动查询微信支付状态
        if (PaymentStatus.isProcessingStatus(order.getStatus())) {
            try {
                updateOrderStatusFromWechat(order);
            } catch (Exception e) {
                log.warn("查询微信支付状态失败: orderNo={}, error={}", orderNo, e.getMessage());
                // 查询失败不影响返回本地状态
            }
        }

        PaymentResponse response = new PaymentResponse();
        response.setOrderId(order.getId());
        response.setOrderNo(order.getOrderNo());
        response.setAmount(order.getAmount());
        response.setPaymentMethod(order.getPaymentMethod());
        response.setStatus(order.getStatus());
        response.setExpiredAt(order.getExpiredAt());
        response.setCreatedAt(order.getCreateTime());

        return response;
    }

    /**
     * 主动查询微信支付状态并更新本地订单状态
     */
    private void updateOrderStatusFromWechat(SubscriptionOrderEntity order) throws WxPayException {
        // 构建查询请求
        WxPayOrderQueryV3Request queryRequest = new WxPayOrderQueryV3Request();
        queryRequest.setOutTradeNo(order.getOrderNo());
        
        // 查询微信支付状态
        WxPayOrderQueryV3Result queryResult = wxPayService.queryOrderV3(queryRequest);
        
        if (queryResult == null) {
            return;
        }
        
        String wechatStatus = queryResult.getTradeState();
        String currentStatus = order.getStatus();
        String newStatus = mapWechatStatusToLocal(wechatStatus);
        
        // 只有状态发生变化时才更新
        if (!currentStatus.equals(newStatus)) {
            order.setStatus(newStatus);
            subscriptionOrderMapper.update(order);
            
            // 发布状态变更事件
            sendPaymentStatusEvent(order.getOrderNo(), newStatus, order.getAmount(), 
                    order.getPaymentMethod(), PaymentStatus.getStatusDescription(newStatus));
        }
    }
    
    /**
     * 映射微信支付状态到本地状态
     */
    private String mapWechatStatusToLocal(String wechatStatus) {
        if (wechatStatus == null) {
            return PaymentStatus.WAITING.getValue();
        }
        
        switch (wechatStatus) {
            case "NOTPAY":
            case "USERPAYING":
                return PaymentStatus.WAITING.getValue(); // 等待支付（包含已扫码但未完成支付的状态）
            case "SUCCESS":
                return PaymentStatus.PAID.getValue();
            case "CLOSED":
            case "REVOKED":
                return PaymentStatus.EXPIRED.getValue();
            case "PAYERROR":
                return PaymentStatus.FAILED.getValue();
            default:
                log.warn("未知的微信支付状态: {}", wechatStatus);
                return PaymentStatus.WAITING.getValue();
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)  
    public boolean handleWechatPayCallback(String callbackData, SignatureHeader header) {
        try {
            if (callbackData == null || callbackData.trim().isEmpty()) {
                log.error("微信支付回调数据为空");
                return false;
            }
            
            // 使用微信支付SDK解密V3回调数据
            WxPayNotifyV3Result notifyResult = wxPayService.parseOrderNotifyV3Result(callbackData, header);
            
            if (notifyResult == null || notifyResult.getResult() == null) {
                log.error("解密微信支付V3回调数据失败");
                return false;
            }
            
            DecryptNotifyResult result = notifyResult.getResult();
            String orderNo = result.getOutTradeNo();
            String transactionId = result.getTransactionId();
            String tradeState = result.getTradeState();
            
            // 检查交易状态
            if (!"SUCCESS".equals(tradeState)) {
                return false;
            }
            
            // 处理支付成功逻辑
            boolean processResult = processPaymentSuccess(orderNo, "wechat");
            
            if (!processResult) {
                log.error("微信支付回调处理失败: orderNo={}, transactionId={}", orderNo, transactionId);
            }
            
            return processResult;

        } catch (WxPayException e) {
            log.error("微信支付SDK解析回调数据失败: {}", e.getMessage(), e);
            return false;
        } catch (Exception e) {
            log.error("处理微信支付回调异常: {}", e.getMessage(), e);
            return false;
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean cancelOrder(String orderNo) {
        try {
            SubscriptionOrderEntity order = subscriptionOrderMapper.selectOneByQuery(
                QueryWrapper.create().where("order_no = ?", orderNo)
            );

            if (order == null) {
                log.warn("取消订单失败，订单不存在: {}", orderNo);
                return false;
            }

            if (!PaymentStatus.isProcessingStatus(order.getStatus())) {
                log.warn("取消订单失败，订单状态不允许取消: orderNo={}, status={}", orderNo, order.getStatus());
                return false;
            }

            order.setStatus(PaymentStatus.CANCELLED.getValue());
            subscriptionOrderMapper.update(order);
            
            // 推送订单取消事件到SSE连接
            sendPaymentStatusEvent(orderNo, PaymentStatus.CANCELLED.getValue(), order.getAmount(), order.getPaymentMethod(), PaymentStatus.CANCELLED.getDescription());

            return true;

        } catch (Exception e) {
            log.error("取消订单异常: orderNo={}", orderNo, e);
            return false;
        }
    }

    /**
     * 创建微信支付V3
     */
    private PaymentResponse createWeChatPayV3(SubscriptionOrderEntity order) {
        try {
            // 创建统一下单请求
            WxPayUnifiedOrderV3Request request = getWxPayUnifiedOrderV3Request(order);

            // 调用微信支付V3 API
            String codeUrl = wxPayService.createOrderV3(TradeTypeEnum.NATIVE, request);

            // 创建支付记录
            PaymentRecordEntity payment = new PaymentRecordEntity();
            payment.setOrderId(order.getId());
            payment.setOrderNo(order.getOrderNo());
            payment.setUserId(order.getUserId());
            payment.setAmount(order.getAmount());
            payment.setPaymentMethod("wechat");
            payment.setStatus(PaymentStatus.WAITING.getValue());
            payment.setQrCode(codeUrl); // 真实的微信支付二维码URL
            
            paymentRecordMapper.insert(payment);

            PaymentResponse response = new PaymentResponse();
            response.setQrCode(codeUrl); // 真实的微信支付二维码URL
            response.setPaymentUrl(codeUrl);
            response.setThirdPartyOrderNo(order.getOrderNo());
            
            return response;

        } catch (WxPayException e) {
            log.error("微信支付V3调用失败: order={}, errorCode={}, errorMsg={}", 
                order, e.getErrCode(), e.getErrCodeDes(), e);
            throw new RuntimeException("微信支付调用失败: " + e.getErrCodeDes());
        } catch (Exception e) {
            log.error("创建微信支付V3失败: order={}", order, e);
            throw new RuntimeException("创建微信支付失败: " + e.getMessage());
        }
    }

    private static WxPayUnifiedOrderV3Request getWxPayUnifiedOrderV3Request(SubscriptionOrderEntity order) {
        WxPayUnifiedOrderV3Request request = new WxPayUnifiedOrderV3Request();
        request.setDescription("Super Agent - " + order.getPlanName());
        request.setOutTradeNo(order.getOrderNo());

        // 金额信息（分为单位）
        WxPayUnifiedOrderV3Request.Amount amount = new WxPayUnifiedOrderV3Request.Amount();
        amount.setTotal(order.getAmount().multiply(new BigDecimal("100")).intValue()); // 转换为分
        amount.setCurrency("CNY");
        request.setAmount(amount);
        return request;
    }

    /**
     * 生成订单号
     */
    private String generateOrderNo() {
        return "ORDER_" + System.currentTimeMillis() + "_" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }


    /**
     * 处理支付成功的业务逻辑
     */
    @Transactional(rollbackFor = Exception.class)
    public boolean processPaymentSuccess(String orderNo, String paymentMethod) {
        try {
            // 1. 查询订单
            SubscriptionOrderEntity order = subscriptionOrderMapper.selectOneByQuery(
                QueryWrapper.create().where("order_no = ?", orderNo)
            );

            if (order == null) {
                log.error("支付成功处理失败，订单不存在: orderNo={}", orderNo);
                return false;
            }

            if (PaymentStatus.PAID.getValue().equals(order.getStatus())) {
                log.warn("订单已处理过支付成功状态: orderNo={}", orderNo);
                return true;
            }

            // 2. 更新订单状态
            order.setStatus(PaymentStatus.PAID.getValue());
            order.setPaidAt(LocalDateTime.now());
            order.setPaymentMethod(paymentMethod);
            subscriptionOrderMapper.update(order);

            // 3. 更新支付记录
            PaymentRecordEntity payment = paymentRecordMapper.selectOneByQuery(
                QueryWrapper.create().where("order_no = ?", orderNo)
            );
            if (payment != null) {
                payment.setStatus("success"); // PaymentRecord表的成功状态
                payment.setThirdPartyTransactionNo("wx_transaction_" + System.currentTimeMillis());
                paymentRecordMapper.update(payment);
            }

            // 4. 发放积分
            SubscriptionPlanEntity plan = subscriptionPlanMapper.selectOneById(order.getPlanId());
            if (plan != null) {
                try {
                    // 发放付费套餐永久积分
                    Long creditAmount = "monthly".equals(order.getBillingCycle()) 
                        ? plan.getMonthlyCreditAmount().longValue()
                        : plan.getYearlyCreditAmount().longValue();
                        
                    userCreditService.grantPaidPlanCredits(
                        order.getUserId(), 
                        creditAmount, 
                        order.getId(), 
                        plan.getPlanName()
                    );
                    
                    // 积分发放成功
                        
                } catch (Exception e) {
                    log.error("发放积分失败: userId={}, planName={}, error={}", 
                        order.getUserId(), plan.getPlanName(), e.getMessage(), e);
                    // 积分发放失败不影响支付成功状态，但需要记录日志用于后续处理
                }
                
                // 激活用户订阅
                try {
                    activateUserSubscription(order, plan);
                } catch (Exception e) {
                    log.error("激活用户订阅失败: userId={}, planId={}, orderId={}, error={}", 
                        order.getUserId(), order.getPlanId(), order.getId(), e.getMessage(), e);
                }
            }
            
            // 推送支付成功事件到SSE连接
            sendPaymentStatusEvent(orderNo, PaymentStatus.PAID.getValue(), order.getAmount(), paymentMethod, PaymentStatus.PAID.getDescription());
            
            return true;
            
        } catch (Exception e) {
            log.error("处理支付成功业务逻辑异常: orderNo={}", orderNo, e);
            return false;
        }
    }
    
    /**
     * 发送支付状态事件（使用Spring事件机制）
     */
    private void sendPaymentStatusEvent(String orderNo, String status, BigDecimal amount, String paymentMethod, String message) {
        try {
            // 创建支付状态事件
            PaymentStatusEvent event = PaymentStatusEvent.builder()
                    .orderNo(orderNo)
                    .status(status)
                    .amount(amount)
                    .paymentMethod(paymentMethod)
                    .message(message)
                    .eventTime(LocalDateTime.now())
                    .build();
            
            // 发布Spring事件，避免循环依赖
            eventPublisher.publishEvent(event);
        } catch (Exception e) {
            // 事件发布失败不影响主业务流程，只记录日志
            log.warn("发布支付状态事件失败: orderNo={}, status={}, error={}", orderNo, status, e.getMessage());
        }
    }

    /**
     * 简化的订阅激活逻辑
     */
    private void activateUserSubscription(SubscriptionOrderEntity order, SubscriptionPlanEntity plan) {
        try {
            // 计算订阅时间
            LocalDateTime startTime = LocalDateTime.now();
            LocalDateTime endTime;
            if ("monthly".equals(order.getBillingCycle())) {
                endTime = startTime.plusMonths(1);
            } else {
                endTime = startTime.plusYears(1);
            }

            // 创建订阅记录
            UserSubscriptionEntity subscription = new UserSubscriptionEntity();
            subscription.setUserId(order.getUserId());
            subscription.setPlanId(order.getPlanId());
            subscription.setStartTime(startTime);
            subscription.setEndTime(endTime);
            subscription.setPaidAmount(order.getAmount());
            subscription.setCreditAmount(plan.getMonthlyCreditAmount()); // 简化，都用月度积分
            subscription.setStatus(SubscriptionStatusEnum.ACTIVE);
            subscription.setPayOrderNo(order.getOrderNo());
            subscription.setRemark("套餐订阅激活");

            userSubscriptionMapper.insert(subscription);
            
            // 订阅激活成功
                
        } catch (Exception e) {
            log.error("激活订阅失败", e);
            throw e;
        }
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public RefundResponse applyRefund(RefundRequest request) {
        try {
            // 1. 查询订单
            SubscriptionOrderEntity order = findOrderByOrderNo(request.getOrderNo());
            if (order == null) {
                throw new RuntimeException("订单不存在: " + request.getOrderNo());
            }
            
            // 2. 检查订单状态是否可以退款
            if (!PaymentStatus.PAID.getValue().equals(order.getStatus())) {
                throw new RuntimeException("订单状态不允许退款: " + order.getStatus());
            }
            
            // 3. 检查退款金额
            if (request.getRefundAmount().compareTo(order.getAmount()) > 0) {
                throw new RuntimeException("退款金额不能超过订单金额");
            }
            
            // 4. 生成退款单号
            String refundNo = "REFUND_" + System.currentTimeMillis() + "_" + 
                             Integer.toHexString((int)(Math.random() * 0x1000000)).toUpperCase();
            
            // 5. 调用微信退款API
            WxPayRefundV3Request wxRefundRequest = buildWxRefundRequest(order, request, refundNo);
            WxPayRefundV3Result wxRefundResult = wxPayService.refundV3(wxRefundRequest);
            
            // 6. 更新订单状态
            order.setStatus(PaymentStatus.REFUND_PROCESSING.getValue());
            subscriptionOrderMapper.update(order);
            
            // 7. 发布退款状态事件
            sendPaymentStatusEvent(order.getOrderNo(), PaymentStatus.REFUND_PROCESSING.getValue(), 
                                 request.getRefundAmount(), "wechat", "退款处理中");
            
            // 8. 构建响应
            RefundResponse response = new RefundResponse();
            response.setOrderNo(order.getOrderNo());
            response.setRefundNo(refundNo);
            response.setRefundId(wxRefundResult.getRefundId());
            response.setRefundStatus(PaymentStatus.REFUND_PROCESSING.getValue());
            response.setRefundAmount(request.getRefundAmount());
            response.setTotalAmount(order.getAmount());
            response.setRefundReason(request.getRefundReason());
            response.setRefundTime(LocalDateTime.now());
            response.setMessage("退款申请提交成功");
            return response;
                    
        } catch (WxPayException e) {
            log.error("微信退款API调用失败: orderNo={}, error={}", request.getOrderNo(), e.getMessage(), e);
            throw new RuntimeException("退款申请失败: " + e.getMessage());
        } catch (Exception e) {
            log.error("申请退款异常: orderNo={}", request.getOrderNo(), e);
            throw new RuntimeException("退款申请失败: " + e.getMessage());
        }
    }
    
    @Override
    public RefundResponse queryRefundStatus(String orderNo) {
        try {
            // 1. 查询订单
            SubscriptionOrderEntity order = findOrderByOrderNo(orderNo);
            if (order == null) {
                throw new RuntimeException("订单不存在: " + orderNo);
            }
            
            // 2. 如果不是退款状态，直接返回
            if (!PaymentStatus.isRefundStatus(order.getStatus())) {
                RefundResponse response = new RefundResponse();
                response.setOrderNo(orderNo);
                response.setRefundStatus(order.getStatus());
                response.setMessage("该订单无退款记录");
                return response;
            }
            
            // 3. 查询微信退款状态 - 需要使用退款单号而不是订单号
            // 这里先暂时返回基本信息，实际应该存储退款单号用于查询
            
            // 4. 构建响应
            RefundResponse response = new RefundResponse();
            response.setOrderNo(orderNo);
            response.setRefundNo("REFUND_" + orderNo);
            response.setRefundStatus(order.getStatus());
            response.setRefundAmount(order.getAmount());
            response.setTotalAmount(order.getAmount());
            response.setMessage("退款状态查询成功");
            
            return response;
                    
        } catch (Exception e) {
            log.error("查询退款状态异常: orderNo={}", orderNo, e);
            throw new RuntimeException("查询退款状态失败: " + e.getMessage());
        }
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean handleWechatRefundCallback(String callbackData, SignatureHeader header) {
        try {
            if (callbackData == null || callbackData.trim().isEmpty()) {
                log.error("微信退款回调数据为空");
                return false;
            }
            
            // 使用微信支付SDK解密V3退款回调数据
            WxPayRefundNotifyV3Result notifyResult = wxPayService.parseRefundNotifyV3Result(callbackData, header);
            
            if (notifyResult == null || notifyResult.getResult() == null) {
                log.error("解密微信退款V3回调数据失败");
                return false;
            }
            
            WxPayRefundNotifyV3Result.DecryptNotifyResult result = notifyResult.getResult();
            String orderNo = result.getOutTradeNo();
            String refundStatus = result.getRefundStatus();
            
            // 更新订单退款状态
            SubscriptionOrderEntity order = findOrderByOrderNo(orderNo);
            if (order != null) {
                String localStatus = mapWechatRefundStatusToLocal(refundStatus);
                order.setStatus(localStatus);
                subscriptionOrderMapper.update(order);
                
                // 发布退款状态变更事件
                // 将微信返回的分转换为元（BigDecimal）
                BigDecimal refundAmount = new BigDecimal(result.getAmount().getRefund()).divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);
                sendPaymentStatusEvent(orderNo, localStatus, refundAmount, 
                                     "wechat", PaymentStatus.getStatusDescription(localStatus));
            }
            
            return true;
            
        } catch (WxPayException e) {
            log.error("微信退款SDK解析回调数据失败: {}", e.getMessage(), e);
            return false;
        } catch (Exception e) {
            log.error("处理微信退款回调异常: {}", e.getMessage(), e);
            return false;
        }
    }
    
    /**
     * 构建微信退款请求
     */
    private WxPayRefundV3Request buildWxRefundRequest(SubscriptionOrderEntity order, 
                                                     RefundRequest request, String refundNo) {
        WxPayRefundV3Request wxRequest = new WxPayRefundV3Request();
        wxRequest.setOutTradeNo(order.getOrderNo());
        wxRequest.setOutRefundNo(refundNo);
        wxRequest.setReason(request.getRefundReason() != null ? request.getRefundReason() : "用户申请退款");
        wxRequest.setNotifyUrl(request.getNotifyUrl());
        
        // 设置金额信息
        WxPayRefundV3Request.Amount amount = new WxPayRefundV3Request.Amount();
        amount.setRefund(request.getRefundAmount().multiply(new BigDecimal("100")).intValue()); // 转为分
        amount.setTotal(order.getAmount().multiply(new BigDecimal("100")).intValue()); // 转为分
        amount.setCurrency("CNY");
        wxRequest.setAmount(amount);
        
        return wxRequest;
    }
    
    /**
     * 映射微信退款状态到本地状态
     */
    private String mapWechatRefundStatusToLocal(String wechatStatus) {
        if (wechatStatus == null) {
            return PaymentStatus.REFUND_FAIL.getValue();
        }
        
        switch (wechatStatus) {
            case "SUCCESS":
                return PaymentStatus.REFUND_SUCCESS.getValue();
            case "REFUNDCLOSE":
                return PaymentStatus.REFUND_CLOSED.getValue();
            case "PROCESSING":
                return PaymentStatus.REFUND_PROCESSING.getValue();
            case "ABNORMAL":
                return PaymentStatus.REFUND_ABNORMAL.getValue();
            default:
                log.warn("未知的微信退款状态: {}", wechatStatus);
                return PaymentStatus.REFUND_FAIL.getValue();
        }
    }
    
    /**
     * 根据订单号查找订单
     */
    private SubscriptionOrderEntity findOrderByOrderNo(String orderNo) {
        QueryWrapper wrapper = QueryWrapper.create()
                .where(SubscriptionOrderEntity::getOrderNo).eq(orderNo);
        return subscriptionOrderMapper.selectOneByQuery(wrapper);
    }
    
    @Override
    public List<PaymentResponse> getUserOrders(Long userId) {
        try {
            // 查询用户的所有订单
            QueryWrapper wrapper = QueryWrapper.create()
                    .where(SubscriptionOrderEntity::getUserId).eq(userId)
                    .orderBy(SubscriptionOrderEntity::getCreateTime, false); // 按创建时间降序
            
            List<SubscriptionOrderEntity> orders = subscriptionOrderMapper.selectListByQuery(wrapper);
            
            List<PaymentResponse> responses = new ArrayList<>();
            for (SubscriptionOrderEntity order : orders) {
                PaymentResponse response = new PaymentResponse();
                response.setOrderNo(order.getOrderNo());
                response.setStatus(order.getStatus());
                response.setAmount(order.getAmount());
                response.setPaymentMethod("wechat");
                response.setCreatedAt(order.getCreateTime());
                response.setExpiredAt(order.getExpiredAt());
                response.setMessage(PaymentStatus.getStatusDescription(order.getStatus()));
                responses.add(response);
            }
            
            return responses;
            
        } catch (Exception e) {
            log.error("查询用户订单列表失败: userId={}", userId, e);
            throw new RuntimeException("查询订单列表失败: " + e.getMessage());
        }
    }
}