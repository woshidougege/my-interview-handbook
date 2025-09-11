package com.noah.superagent.service.impl;

import com.mybatisflex.core.query.QueryWrapper;
import com.noah.superagent.common.dto.request.CreateOrderRequest;
import com.noah.superagent.common.dto.response.PaymentResponse;
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
import com.github.binarywang.wxpay.bean.result.enums.TradeTypeEnum;
import com.github.binarywang.wxpay.exception.WxPayException;
import com.github.binarywang.wxpay.service.WxPayService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
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
            order.setStatus("pending");
            order.setPaymentMethod("wechat");
            order.setExpiredAt(LocalDateTime.now().plusMinutes(15)); // 15分钟后过期
            
            subscriptionOrderMapper.insert(order);

            // 5. 创建微信支付
            PaymentResponse response = createWeChatPayV3(order);
            response.setOrderId(order.getId());
            response.setOrderNo(orderNo);
            response.setAmount(amount);
            response.setPaymentMethod("wechat");
            response.setStatus("pending");
            response.setExpiredAt(order.getExpiredAt());
            response.setCreatedTime(order.getCreateTime());

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

        PaymentResponse response = new PaymentResponse();
        response.setOrderId(order.getId());
        response.setOrderNo(order.getOrderNo());
        response.setAmount(order.getAmount());
        response.setPaymentMethod(order.getPaymentMethod());
        response.setStatus(order.getStatus());
        response.setExpiredAt(order.getExpiredAt());
        response.setCreatedTime(order.getCreateTime());

        return response;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean handleWechatPayCallback(String callbackData) {
        try {
            // TODO: 实现真实的微信支付V3回调处理
            // 这里应该验证微信支付回调签名，解析回调数据
            log.info("处理微信支付回调: {}", callbackData);

            // 从回调数据中提取订单号（简化实现）
            String orderNo = extractOrderNoFromCallback(callbackData);
            if (orderNo == null) {
                log.error("无法从微信支付回调中提取订单号");
                return false;
            }

            return processPaymentSuccess(orderNo, "wechat");

        } catch (Exception e) {
            log.error("处理微信支付回调异常", e);
            return false;
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean handleAlipayCallback(String callbackData) {
        // 不再支持支付宝支付
        log.warn("收到支付宝回调，但当前系统只支持微信支付: {}", callbackData);
        return false;
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

            if (!"pending".equals(order.getStatus())) {
                log.warn("取消订单失败，订单状态不允许取消: orderNo={}, status={}", orderNo, order.getStatus());
                return false;
            }

            order.setStatus("cancelled");
            subscriptionOrderMapper.update(order);

            log.info("订单取消成功: {}", orderNo);
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
            log.info("创建微信支付V3: order={}", order);

            // 创建统一下单请求
            WxPayUnifiedOrderV3Request request = getWxPayUnifiedOrderV3Request(order);

            // 调用微信支付V3 API
            String codeUrl = wxPayService.createOrderV3(TradeTypeEnum.NATIVE, request);
            
            log.info("微信支付V3下单成功: orderNo={}, codeUrl={}", order.getOrderNo(), codeUrl);

            // 创建支付记录
            PaymentRecordEntity payment = new PaymentRecordEntity();
            payment.setOrderId(order.getId());
            payment.setOrderNo(order.getOrderNo());
            payment.setUserId(order.getUserId());
            payment.setAmount(order.getAmount());
            payment.setPaymentMethod("wechat");
            payment.setStatus("pending");
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
     * 从回调数据中提取订单号
     */
    private String extractOrderNoFromCallback(String callbackData) {
        // 简化实现：从模拟回调数据中提取
        if (callbackData != null && callbackData.contains("mock_callback_data_")) {
            return callbackData.replace("mock_callback_data_", "");
        }
        
        // TODO: 实现真实的微信V3回调数据解析
        return null;
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

            if ("paid".equals(order.getStatus())) {
                log.warn("订单已处理过支付成功状态: orderNo={}", orderNo);
                return true;
            }

            // 2. 更新订单状态
            order.setStatus("paid");
            order.setPaidAt(LocalDateTime.now());
            order.setPaymentMethod(paymentMethod);
            subscriptionOrderMapper.update(order);

            // 3. 更新支付记录
            PaymentRecordEntity payment = paymentRecordMapper.selectOneByQuery(
                QueryWrapper.create().where("order_no = ?", orderNo)
            );
            if (payment != null) {
                payment.setStatus("success");
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
                    
                    log.info("发放积分成功: userId={}, planName={}, creditAmount={}", 
                        order.getUserId(), plan.getPlanName(), creditAmount);
                        
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