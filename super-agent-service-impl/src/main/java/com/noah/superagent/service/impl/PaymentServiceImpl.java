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
    public boolean handleWechatPayCallback(String callbackData) {
        // TODO: 实现微信支付回调处理
        log.info("处理微信支付回调: {}", callbackData);
        return true;
    }

    @Override
    public boolean handleAlipayCallback(String callbackData) {
        // TODO: 实现支付宝支付回调处理
        log.info("处理支付宝支付回调: {}", callbackData);
        return true;
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
}
