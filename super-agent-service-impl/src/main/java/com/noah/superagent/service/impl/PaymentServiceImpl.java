package com.noah.superagent.service.impl;

import com.mybatisflex.core.query.QueryWrapper;
import com.noah.superagent.common.dto.request.CreateOrderRequest;
import com.noah.superagent.common.dto.response.PaymentResponse;
import com.noah.superagent.dao.entity.PaymentRecordEntity;
import com.noah.superagent.dao.entity.SubscriptionOrderEntity;
import com.noah.superagent.dao.entity.SubscriptionPlanEntity;
import com.noah.superagent.dao.mapper.PaymentRecordMapper;
import com.noah.superagent.dao.mapper.SubscriptionOrderMapper;
import com.noah.superagent.dao.mapper.SubscriptionPlanMapper;
import com.noah.superagent.service.PaymentService;
import com.noah.superagent.service.UserCreditService;
import com.noah.superagent.dao.entity.UserSubscriptionEntity;
import com.noah.superagent.dao.mapper.UserSubscriptionMapper;
import com.noah.superagent.common.enums.SubscriptionStatusEnum;
import com.wechat.pay.java.core.Config;
import com.wechat.pay.java.core.RSAAutoCertificateConfig;
import com.wechat.pay.java.service.payments.nativepay.NativePayService;
import com.wechat.pay.java.service.payments.nativepay.model.PrepayRequest;
import com.wechat.pay.java.service.payments.nativepay.model.PrepayResponse;
import com.wechat.pay.java.service.payments.nativepay.model.Amount;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 支付服务实现
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

    @Value("${payment.wechat.app-id:}")
    private String wechatAppId;

    @Value("${payment.wechat.mch-id:}")
    private String wechatMchId;

    @Value("${payment.wechat.private-key-path:}")
    private String wechatPrivateKeyPath;

    @Value("${payment.wechat.merchant-serial-number:}")
    private String wechatMerchantSerialNumber;

    @Value("${payment.wechat.api-v3-key:}")
    private String wechatApiV3Key;

    @Value("${payment.wechat.notify-url:}")
    private String wechatNotifyUrl;

    @Value("${payment.alipay.app-id:}")
    private String alipayAppId;

    @Value("${payment.alipay.private-key:}")
    private String alipayPrivateKey;

    @Value("${payment.alipay.public-key:}")
    private String alipayPublicKey;

    @Value("${payment.alipay.notify-url:}")
    private String alipayNotifyUrl;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public PaymentResponse createOrderAndPay(Long userId, CreateOrderRequest request) {
        try {
            // 1. 查询套餐信息
            SubscriptionPlanEntity plan = subscriptionPlanMapper.selectOneById(request.getPlanId());
            if (plan == null) {
                throw new RuntimeException("套餐不存在");
            }

            // 2. 计算订单金额
            BigDecimal amount = "monthly".equals(request.getBillingCycle()) 
                ? plan.getMonthlyPrice() 
                : plan.getYearlyPrice();

            // 3. 创建订单
            String orderNo = generateOrderNo();
            SubscriptionOrderEntity order = new SubscriptionOrderEntity();
            order.setOrderNo(orderNo);
            order.setUserId(userId);
            order.setPlanId(request.getPlanId());
            order.setPlanName(plan.getPlanName());
            order.setAmount(amount);
            order.setBillingCycle(request.getBillingCycle());
            order.setStatus("pending");
            order.setPaymentMethod(request.getPaymentMethod());
            order.setExpiredAt(LocalDateTime.now().plusMinutes(15)); // 15分钟后过期
            
            subscriptionOrderMapper.insert(order);

            // 4. 发起支付
            PaymentResponse response = new PaymentResponse();
            response.setOrderId(order.getId());
            response.setOrderNo(orderNo);
            response.setAmount(amount);
            response.setPaymentMethod(request.getPaymentMethod());
            response.setStatus("pending");
            response.setExpiredAt(order.getExpiredAt());
            response.setCreatedTime(order.getCreateTime());

            if ("wechat".equals(request.getPaymentMethod())) {
                response = createWechatPayment(order, response);
            } else if ("alipay".equals(request.getPaymentMethod())) {
                response = createAlipayment(order, response);
            }

            return response;

        } catch (Exception e) {
            log.error("创建订单支付失败: userId={}, request={}", userId, request, e);
            throw new RuntimeException("创建订单失败: " + e.getMessage());
        }
    }

    @Override
    public PaymentResponse queryPaymentStatus(String orderNo) {
        // 查询订单
        QueryWrapper wrapper = QueryWrapper.create()
            .eq(SubscriptionOrderEntity::getOrderNo, orderNo);
        SubscriptionOrderEntity order = subscriptionOrderMapper.selectOneByQuery(wrapper);
        
        if (order == null) {
            throw new RuntimeException("订单不存在");
        }

        // 查询支付记录
        QueryWrapper paymentWrapper = QueryWrapper.create()
            .eq(PaymentRecordEntity::getOrderNo, orderNo)
            .orderBy("create_time", false);
        PaymentRecordEntity payment = paymentRecordMapper.selectOneByQuery(paymentWrapper);

        PaymentResponse response = new PaymentResponse();
        response.setOrderId(order.getId());
        response.setOrderNo(orderNo);
        response.setAmount(order.getAmount());
        response.setPaymentMethod(order.getPaymentMethod());
        response.setStatus(order.getStatus());
        response.setExpiredAt(order.getExpiredAt());
        response.setCreatedTime(order.getCreateTime());

        if (payment != null) {
            response.setQrCode(payment.getQrCode());
            response.setPaymentUrl(payment.getPaymentUrl());
            response.setThirdPartyOrderNo(payment.getThirdPartyOrderNo());
        }

        return response;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean handleWechatPayCallback(String callbackData) {
        try {
            // TODO: [后续开发] 实现真实的微信支付回调处理
            // 1. 验证回调签名
            // 2. 解析回调数据
            // 3. 验证订单金额和状态
            // 4. 调用微信API确认支付状态
            
            log.info("模拟微信支付回调成功: {}", callbackData);
            
            // 从回调数据中提取订单号
            String orderNo = extractOrderNoFromCallback(callbackData);
            if (orderNo == null) {
                log.error("无法从微信支付回调中提取订单号");
                return false;
            }
            
            // 模拟支付成功：直接处理订单完成逻辑
            return processPaymentSuccess(orderNo, "wechat");
            
        } catch (Exception e) {
            log.error("处理微信支付回调异常", e);
            return false;
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean handleAlipayCallback(String callbackData) {
        try {
            // TODO: [后续开发] 实现真实的支付宝支付回调处理
            // 1. 验证回调签名
            // 2. 解析回调数据  
            // 3. 验证订单金额和状态
            // 4. 调用支付宝API确认支付状态
            
            log.info("模拟支付宝支付回调成功: {}", callbackData);
            
            // 从回调数据中提取订单号
            String orderNo = extractOrderNoFromCallback(callbackData);
            if (orderNo == null) {
                log.error("无法从支付宝支付回调中提取订单号");
                return false;
            }
            
            // 模拟支付成功：直接处理订单完成逻辑
            return processPaymentSuccess(orderNo, "alipay");
            
        } catch (Exception e) {
            log.error("处理支付宝支付回调异常", e);
            return false;
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean cancelOrder(String orderNo) {
        try {
            QueryWrapper wrapper = QueryWrapper.create()
                .eq(SubscriptionOrderEntity::getOrderNo, orderNo);
            SubscriptionOrderEntity order = subscriptionOrderMapper.selectOneByQuery(wrapper);
            
            if (order == null) {
                return false;
            }

            if (!"pending".equals(order.getStatus())) {
                throw new RuntimeException("订单状态不允许取消");
            }

            order.setStatus("cancelled");
            subscriptionOrderMapper.update(order);

            return true;
        } catch (Exception e) {
            log.error("取消订单失败: orderNo={}", orderNo, e);
            return false;
        }
    }

    /**
     * 创建微信支付
     */
    private PaymentResponse createWechatPayment(SubscriptionOrderEntity order, PaymentResponse response) {
        try {
            // 初始化微信支付配置
            Config config = new RSAAutoCertificateConfig.Builder()
                .merchantId(wechatMchId)
                .privateKeyFromPath(wechatPrivateKeyPath)
                .merchantSerialNumber(wechatMerchantSerialNumber)
                .apiV3Key(wechatApiV3Key)
                .build();

            NativePayService service = new NativePayService.Builder().config(config).build();

            // 创建预支付交易单
            PrepayRequest request = new PrepayRequest();
            request.setAppid(wechatAppId);
            request.setMchid(wechatMchId);
            request.setDescription(order.getPlanName());
            request.setOutTradeNo(order.getOrderNo());
            request.setNotifyUrl(wechatNotifyUrl);
            
            Amount amount = new Amount();
            amount.setTotal(order.getAmount().multiply(new BigDecimal("100")).intValue()); // 转换为分
            request.setAmount(amount);

            PrepayResponse prepayResponse = service.prepay(request);

            // 保存支付记录
            PaymentRecordEntity payment = new PaymentRecordEntity();
            payment.setOrderId(order.getId());
            payment.setOrderNo(order.getOrderNo());
            payment.setUserId(order.getUserId());
            payment.setAmount(order.getAmount());
            payment.setPaymentMethod("wechat");
            payment.setQrCode(prepayResponse.getCodeUrl());
            payment.setThirdPartyOrderNo(order.getOrderNo());
            payment.setStatus("pending");

            paymentRecordMapper.insert(payment);

            response.setQrCode(prepayResponse.getCodeUrl());
            response.setThirdPartyOrderNo(order.getOrderNo());

        } catch (Exception e) {
            log.error("创建微信支付失败: order={}", order, e);
            throw new RuntimeException("创建微信支付失败: " + e.getMessage());
        }

        return response;
    }

    /**
     * 创建支付宝支付
     */
    private PaymentResponse createAlipayment(SubscriptionOrderEntity order, PaymentResponse response) {
        try {
            // TODO: 实现支付宝支付逻辑
            log.info("创建支付宝支付: order={}", order);
            
            // 暂时返回模拟数据
            PaymentRecordEntity payment = new PaymentRecordEntity();
            payment.setOrderId(order.getId());
            payment.setOrderNo(order.getOrderNo());
            payment.setUserId(order.getUserId());
            payment.setAmount(order.getAmount());
            payment.setPaymentMethod("alipay");
            payment.setQrCode("https://qr.alipay.com/mock_qr_code");
            payment.setThirdPartyOrderNo(order.getOrderNo());
            payment.setStatus("pending");

            paymentRecordMapper.insert(payment);

            response.setQrCode("https://qr.alipay.com/mock_qr_code");
            response.setThirdPartyOrderNo(order.getOrderNo());

        } catch (Exception e) {
            log.error("创建支付宝支付失败: order={}", order, e);
            throw new RuntimeException("创建支付宝支付失败: " + e.getMessage());
        }

        return response;
    }

    /**
     * 生成订单号
     */
    private String generateOrderNo() {
        return "ORDER" + System.currentTimeMillis() + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }
    
    /**
     * 从回调数据中提取订单号（模拟实现）
     * TODO: [后续开发] 根据真实的微信/支付宝回调数据格式解析订单号
     */
    private String extractOrderNoFromCallback(String callbackData) {
        // 模拟实现：从mock回调数据中提取订单号
        // 格式: mock_callback_data_ORDER123456
        log.info("模拟从回调数据中提取订单号: {}", callbackData);
        
        if (callbackData != null && callbackData.startsWith("mock_callback_data_")) {
            String orderNo = callbackData.replace("mock_callback_data_", "");
            log.info("提取到订单号: {}", orderNo);
            return orderNo;
        }
        
        // TODO: [后续开发] 实际开发中需要根据具体的回调数据格式解析
        // 微信支付回调数据格式解析
        // 支付宝回调数据格式解析
        
        log.warn("无法从回调数据中提取订单号: {}", callbackData);
        return null;
    }
    
    /**
     * 处理支付成功业务逻辑
     */
    private boolean processPaymentSuccess(String orderNo, String paymentMethod) {
        try {
            // 1. 查找订单
            QueryWrapper wrapper = QueryWrapper.create()
                .eq(SubscriptionOrderEntity::getOrderNo, orderNo);
            SubscriptionOrderEntity order = subscriptionOrderMapper.selectOneByQuery(wrapper);
            
            if (order == null) {
                log.error("支付成功但找不到订单: orderNo={}", orderNo);
                return false;
            }
            
            if (!"pending".equals(order.getStatus())) {
                log.warn("订单状态不是待支付: orderNo={}, status={}", orderNo, order.getStatus());
                return true; // 已处理过，返回成功
            }
            
            // 2. 更新订单状态为已支付
            order.setStatus("paid");
            order.setPaidAt(LocalDateTime.now());
            subscriptionOrderMapper.update(order);
            
            // 3. 更新支付记录状态
            QueryWrapper paymentWrapper = QueryWrapper.create()
                .eq(PaymentRecordEntity::getOrderNo, orderNo);
            PaymentRecordEntity payment = paymentRecordMapper.selectOneByQuery(paymentWrapper);
            if (payment != null) {
                payment.setStatus("success");
                payment.setPaidAt(LocalDateTime.now());
                paymentRecordMapper.update(payment);
            }
            
            // 4. 查询套餐信息，发放积分
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
                    
                    log.info("发放积分成功: userId={}, planName={}, creditAmount={}", 
                        order.getUserId(), plan.getPlanName(), creditAmount);
                        
                } catch (Exception e) {
                    log.error("发放积分失败: userId={}, planName={}, error={}", 
                        order.getUserId(), plan.getPlanName(), e.getMessage(), e);
                    // 积分发放失败不影响支付成功状态，但需要记录日志用于后续处理
                }
                
                // 简化的订阅激活逻辑
                try {
                    activateUserSubscription(order, plan);
                } catch (Exception e) {
                    log.error("激活用户订阅失败: userId={}, planId={}, orderId={}, error={}", 
                        order.getUserId(), order.getPlanId(), order.getId(), e.getMessage(), e);
                }
            }
            
            log.info("支付成功处理完成: orderNo={}, userId={}, amount={}", 
                orderNo, order.getUserId(), order.getAmount());
            
            return true;
            
        } catch (Exception e) {
            log.error("处理支付成功业务逻辑异常: orderNo={}", orderNo, e);
            return false;
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
            
            log.info("用户订阅激活成功: userId={}, planId={}, subscriptionId={}", 
                order.getUserId(), order.getPlanId(), subscription.getId());
                
        } catch (Exception e) {
            log.error("激活订阅失败", e);
            throw e;
        }
    }
}
