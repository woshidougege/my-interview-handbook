package com.noah.superagent.service.impl;

import com.mybatisflex.core.query.QueryWrapper;
import com.noah.superagent.common.dto.request.CreateOrderRequest;
import com.noah.superagent.common.dto.response.PaymentResponse;
import com.noah.superagent.common.dto.RefundRequest;
import com.noah.superagent.common.dto.RefundResponse;
import com.noah.superagent.common.dto.PaymentStatusEvent;
import com.noah.superagent.common.constants.PaymentStatus;
import com.noah.superagent.common.config.CreditPurchaseConfig;
import org.springframework.context.ApplicationEventPublisher;
import com.noah.superagent.dao.entity.PaymentRecordEntity;
import com.noah.superagent.dao.entity.SubscriptionOrderEntity;
import com.noah.superagent.dao.entity.UserSubscriptionEntity;
import com.noah.superagent.dao.mapper.PaymentRecordMapper;
import com.noah.superagent.dao.mapper.SubscriptionOrderMapper;
import com.noah.superagent.dao.mapper.UserSubscriptionMapper;
import com.noah.superagent.service.PaymentService;
import com.noah.superagent.service.SubscriptionPlanService;
import com.noah.superagent.service.UserCreditService;
import com.noah.superagent.service.UserSubscriptionService;
import com.noah.superagent.model.SubscriptionPlanDTO;
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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
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
    private final SubscriptionPlanService subscriptionPlanService;
    private final UserCreditService userCreditService;
    private final UserSubscriptionMapper userSubscriptionMapper;
    private final UserSubscriptionService userSubscriptionService;
    private final WxPayService wxPayService;
    private final CreditPurchaseConfig creditPurchaseConfig;
    
    // 使用ApplicationEventPublisher发布支付状态事件，避免循环依赖
    @Autowired
    private ApplicationEventPublisher eventPublisher;
    
    // 套餐积分配置
    @Value("#{${super-agent.billing.subscription.plan-credits:{}}}")
    private Map<String, Integer> planCreditsConfig;
    
    // 自注入以解决事务自调用问题
    @Lazy
    @Autowired
    private PaymentService selfProxy;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public PaymentResponse createOrderAndPay(Long userId, CreateOrderRequest request) {
        try {
            // 1. 验证支付方式 - 只支持微信支付
            if (!"wechat".equals(request.getPaymentMethod())) {
                throw new RuntimeException("当前只支持微信支付");
            }

            // 2. 查询套餐信息
            SubscriptionPlanDTO plan = subscriptionPlanService.getPlanById(request.getPlanId());
            if (plan == null) {
                throw new RuntimeException("套餐不存在");
            }

            // 3. 计算订单金额
            BigDecimal amount;
            String actualPlanName = plan.getPlanName();
            
            // 如果是购买积分套餐(ID=100)，需要特殊处理
            if (request.getPlanId() == 100L) {
                if (request.getCreditPackageId() == null || request.getCreditPackageId().isEmpty()) {
                    throw new RuntimeException("购买积分时必须指定积分包ID");
                }
                
                // 从积分购买配置中获取对应的积分包
                CreditPurchaseConfig.CreditPackageConfig creditPackage = creditPurchaseConfig.getPackages()
                    .stream()
                    .filter(pkg -> pkg.getId().equals(request.getCreditPackageId()))
                    .findFirst()
                    .orElseThrow(() -> new RuntimeException("积分包不存在: " + request.getCreditPackageId()));
                
                amount = creditPackage.getPrice();
                actualPlanName = creditPackage.getName(); // 使用积分包名称
            } else {
                // 普通套餐订阅
                amount = "monthly".equals(request.getBillingCycle()) 
                    ? plan.getMonthlyPrice() 
                    : plan.getYearlyPrice();
            }

            // 4. 创建订单
            String orderNo = generateOrderNo();
            SubscriptionOrderEntity order = new SubscriptionOrderEntity();
            order.setOrderNo(orderNo);
            order.setUserId(userId);
            order.setPlanId(request.getPlanId());
            order.setPlanName(actualPlanName);
            order.setAmount(amount);
            order.setBillingCycle(request.getBillingCycle());
            order.setStatus(PaymentStatus.WAITING.getValue());
            order.setPaymentMethod("wechat");
            order.setExpiredAt(LocalDateTime.now().plusMinutes(15)); // 15分钟后过期
            
            // 如果是积分购买，在备注中保存积分包ID
            if (request.getPlanId() == 100L && request.getCreditPackageId() != null) {
                order.setRemark("credit_package_id:" + request.getCreditPackageId());
            }
            
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
            case "SUCCESS":         // 支付成功
                return PaymentStatus.PAID.getValue();
            case "REFUND":          // 转入退款
                return PaymentStatus.REFUND_PROCESSING.getValue();
            case "NOTPAY":          // 未支付
                return PaymentStatus.WAITING.getValue();
            case "CLOSED":          // 已关闭
                return PaymentStatus.CANCELLED.getValue();
            case "REVOKED":         // 已撤销（刷卡支付）
                return PaymentStatus.CANCELLED.getValue();
            case "USERPAYING":      // 用户支付中
                return PaymentStatus.PENDING.getValue();
            case "PAYERROR":        // 支付失败
                return PaymentStatus.FAILED.getValue();
            default:
                log.warn("未知的微信支付状态: {}, 默认返回waiting", wechatStatus);
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
     * 从订单备注中提取积分包ID
     */
    private String extractCreditPackageId(String remark) {
        if (remark == null || remark.isEmpty()) {
            return null;
        }
        
        String prefix = "credit_package_id:";
        if (remark.startsWith(prefix)) {
            return remark.substring(prefix.length());
        }
        
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

            // 4. 处理积分发放和订阅激活
            SubscriptionPlanDTO plan = subscriptionPlanService.getPlanById(order.getPlanId());
            if (plan != null) {
                // 检查是否是积分购买订单
                if (order.getPlanId() == 100L) {
                    // 积分购买订单的特殊处理
                    try {
                        // 从备注中提取积分包ID
                        String creditPackageId = extractCreditPackageId(order.getRemark());
                        if (creditPackageId != null) {
                            // 从积分购买配置中获取积分数量
                            CreditPurchaseConfig.CreditPackageConfig creditPackage = creditPurchaseConfig.getPackages()
                                .stream()
                                .filter(pkg -> pkg.getId().equals(creditPackageId))
                                .findFirst()
                                .orElse(null);
                            
                            if (creditPackage != null) {
                                // 发放积分（使用永久积分类型）
                                userCreditService.grantPaidPlanCredits(
                                    order.getUserId(),
                                    creditPackage.getCreditsAmount(),
                                    order.getId(),
                                    creditPackage.getName()
                                );
                                log.info("积分购买发放成功 - packageId: {}, creditAmount: {}", 
                                    creditPackageId, creditPackage.getCreditsAmount());
                            } else {
                                log.error("积分包配置不存在: creditPackageId={}", creditPackageId);
                            }
                        } else {
                            log.error("无法从订单备注中提取积分包ID: remark={}", order.getRemark());
                        }
                    } catch (Exception e) {
                        log.error("积分购买发放失败: userId={}, orderId={}, error={}", 
                            order.getUserId(), order.getId(), e.getMessage(), e);
                        // 积分发放失败不影响支付成功状态，但需要记录日志用于后续处理
                    }
                    // 注意：积分购买不需要激活订阅
                } else {
                    // 普通订阅套餐的处理
                    try {
                        // 从配置文件获取套餐积分数量
                        String planIdStr = String.valueOf(order.getPlanId());
                        Integer creditAmount = planCreditsConfig.get(planIdStr);
                        
                        if (creditAmount != null && creditAmount > 0) {
                            userCreditService.grantPaidPlanCredits(
                                order.getUserId(), 
                                creditAmount.longValue(), 
                                order.getId(), 
                                plan.getPlanName()
                            );
                            log.info("套餐积分发放成功 - planId: {}, creditAmount: {}", order.getPlanId(), creditAmount);
                        } else {
                            log.info("套餐不包含积分或积分为0 - planId: {}", order.getPlanId());
                        }
                            
                    } catch (Exception e) {
                        log.error("发放积分失败: userId={}, planName={}, error={}", 
                            order.getUserId(), plan.getPlanName(), e.getMessage(), e);
                        // 积分发放失败不影响支付成功状态，但需要记录日志用于后续处理
                    }
                    
                    // 激活用户订阅（使用智能升级逻辑）
                    try {
                        activateUserSubscriptionWithUpgradeLogic(order);
                    } catch (Exception e) {
                        log.error("激活用户订阅失败: userId={}, planId={}, orderId={}, error={}", 
                            order.getUserId(), order.getPlanId(), order.getId(), e.getMessage(), e);
                    }
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
     * 使用智能升级逻辑激活用户订阅
     */
    private void activateUserSubscriptionWithUpgradeLogic(SubscriptionOrderEntity order) {
        try {
            log.info("使用智能升级逻辑激活订阅 - userId: {}, planId: {}, billingCycle: {}", 
                    order.getUserId(), order.getPlanId(), order.getBillingCycle());

            userSubscriptionService.activateSubscriptionWithUpgradeLogic(
                    order.getUserId(),
                    order.getPlanId(),
                    order.getBillingCycle(),
                    order.getOrderNo(),
                    order.getAmount()
            );

            log.info("智能订阅激活成功 - userId: {}, planId: {}", order.getUserId(), order.getPlanId());
            
        } catch (Exception e) {
            log.error("智能订阅激活失败 - userId: {}, planId: {}", order.getUserId(), order.getPlanId(), e);
            throw e;
        }
    }

    /**
     * 原有的简化订阅激活逻辑（保留作为备用）
     */
    private void activateUserSubscription(SubscriptionOrderEntity order) {
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
            // 从配置文件获取积分数量用于记录
            String planIdStr = String.valueOf(order.getPlanId());
            Integer creditAmount = planCreditsConfig.get(planIdStr);
            subscription.setCreditAmount(creditAmount != null ? new BigDecimal(creditAmount) : BigDecimal.ZERO);
            subscription.setStatus(SubscriptionStatusEnum.ACTIVE);
            subscription.setPayOrderNo(order.getOrderNo());
            subscription.setRemark("套餐订阅激活");

            userSubscriptionMapper.insert(subscription);
            
            log.info("订阅激活成功 - userId: {}, planId: {}, endTime: {}", 
                    order.getUserId(), order.getPlanId(), endTime);
                
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
            
            // 2. 同步微信支付状态
            order = syncOrderStatusFromWechat(order);
            
            // 3. 检查订单状态是否可以退款
            if (!PaymentStatus.PAID.getValue().equals(order.getStatus())) {
                throw new RuntimeException("订单当前状态不允许退款，状态: " + getStatusDisplayName(order.getStatus()));
            }
            
            // 4. 检查退款金额
            if (request.getRefundAmount().compareTo(order.getAmount()) > 0) {
                throw new RuntimeException("退款金额不能超过订单金额");
            }
            
            // 5. 生成退款单号
            String refundNo = "REFUND_" + System.currentTimeMillis() + "_" + 
                             Integer.toHexString((int)(Math.random() * 0x1000000)).toUpperCase();
            
            // 6. 调用微信退款API
            WxPayRefundV3Request wxRefundRequest = buildWxRefundRequest(order, request, refundNo);
            WxPayRefundV3Result wxRefundResult = wxPayService.refundV3(wxRefundRequest);
            
            // 7. 更新订单状态
            order.setStatus(PaymentStatus.REFUND_PROCESSING.getValue());
            subscriptionOrderMapper.update(order);
            
            // 8. 发布退款状态事件
            sendPaymentStatusEvent(order.getOrderNo(), PaymentStatus.REFUND_PROCESSING.getValue(), 
                                 request.getRefundAmount(), "wechat", "退款处理中");
            
            // 9. 构建响应
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
            
            // 根据错误信息提供友好的提示
            String errorMessage = e.getMessage();
            String friendlyMessage;
            
            if (errorMessage.contains("基本账户余额不足")) {
                friendlyMessage = "商户账户余额不足，请联系客服充值后重试";
            } else if (errorMessage.contains("订单不存在")) {
                friendlyMessage = "订单信息异常，请稍后重试";
            } else if (errorMessage.contains("订单状态不正确")) {
                friendlyMessage = "订单状态异常，无法退款";
            } else if (errorMessage.contains("退款金额超限")) {
                friendlyMessage = "退款金额超过限制，请检查退款金额";
            } else if (errorMessage.contains("重复退款")) {
                friendlyMessage = "该订单已申请退款，请勿重复操作";
            } else {
                friendlyMessage = "退款申请失败，请稍后重试或联系客服";
            }
            
            throw new RuntimeException(friendlyMessage + "（错误代码：" + errorMessage + "）");
        } catch (Exception e) {
            log.error("申请退款异常: orderNo={}", request.getOrderNo(), e);
            throw new RuntimeException("系统异常，退款申请失败，请稍后重试或联系客服");
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
            
            // 2. 同步微信支付状态
            order = syncOrderStatusFromWechat(order);
            
            // 3. 如果不是退款状态，检查是否为已支付状态
            if (!PaymentStatus.isRefundStatus(order.getStatus())) {
                RefundResponse response = new RefundResponse();
                response.setOrderNo(orderNo);
                response.setRefundStatus(order.getStatus());
                if (PaymentStatus.PAID.getValue().equals(order.getStatus())) {
                    response.setMessage("订单已支付，可申请退款");
                } else {
                    response.setMessage("该订单当前状态: " + getStatusDisplayName(order.getStatus()) + "，无退款记录");
                }
                return response;
            }
            
            // 4. 构建退款响应
            RefundResponse response = new RefundResponse();
            response.setOrderNo(orderNo);
            response.setRefundNo("REFUND_" + orderNo);  // 实际项目中应从数据库获取真实退款单号
            response.setRefundStatus(order.getStatus());
            response.setRefundAmount(order.getAmount());  // 实际项目中应从退款记录获取
            response.setTotalAmount(order.getAmount());
            response.setRefundTime(order.getUpdateTime());  // 实际项目中应从退款记录获取
            response.setMessage("退款状态: " + getStatusDisplayName(order.getStatus()));
            
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
    
    /**
     * 从微信支付同步订单状态
     * 只有非最终状态的订单才会同步，避免不必要的API调用
     */
    private SubscriptionOrderEntity syncOrderStatusFromWechat(SubscriptionOrderEntity order) {
        if (order == null) {
            return null;
        }
        
        // 检查是否为最终状态，最终状态不需要同步
        PaymentStatus currentStatus = PaymentStatus.fromValue(order.getStatus());
        if (currentStatus != null && currentStatus.isFinalStatus()) {
            log.debug("订单状态为最终状态，无需同步: orderNo={}, status={}", order.getOrderNo(), order.getStatus());
            return order;
        }
        
        try {
            log.info("开始同步微信支付订单状态: orderNo={}, currentStatus={}", order.getOrderNo(), order.getStatus());
            
            // 调用微信API查询订单状态
            WxPayOrderQueryV3Request queryRequest = new WxPayOrderQueryV3Request();
            queryRequest.setOutTradeNo(order.getOrderNo());
            queryRequest.setMchid(wxPayService.getConfig().getMchId());
            
            WxPayOrderQueryV3Result queryResult = wxPayService.queryOrderV3(queryRequest);
            String wechatStatus = queryResult.getTradeState();
            
            // 映射微信状态到本地状态
            String localStatus = mapWechatStatusToLocal(wechatStatus);
            
            // 如果状态发生变化，更新数据库
            if (!localStatus.equals(order.getStatus())) {
                log.info("检测到状态变化，更新订单状态: orderNo={}, oldStatus={}, newStatus={}, wechatStatus={}", 
                        order.getOrderNo(), order.getStatus(), localStatus, wechatStatus);
                
                String oldStatus = order.getStatus();
                order.setStatus(localStatus);
                
                // 如果变为支付成功状态，设置支付时间
                if (PaymentStatus.PAID.getValue().equals(localStatus) && order.getPaidAt() == null) {
                    order.setPaidAt(LocalDateTime.now());
                }
                
                // 以事务方式更新所有相关表，确保数据一致性
                selfProxy.updateAllRelatedTablesWithTransaction(order, localStatus, queryResult);
                
                // 发送状态变化事件
                sendPaymentStatusEvent(order.getOrderNo(), localStatus, order.getAmount(), "wechat", 
                                     "状态已从微信同步: " + getStatusDisplayName(localStatus));
                
                log.info("订单状态同步完成: orderNo={}, oldStatus={}, newStatus={}", 
                        order.getOrderNo(), oldStatus, localStatus);
            } else {
                log.debug("订单状态无变化: orderNo={}, status={}", order.getOrderNo(), order.getStatus());
            }
            
            return order;
            
        } catch (WxPayException e) {
            log.warn("同步微信支付状态失败，继续使用本地状态: orderNo={}, error={}", order.getOrderNo(), e.getMessage());
            
            // 如果是已支付订单查询失败，不应该改变状态
            if (order.getPaidAt() != null && !PaymentStatus.PAID.getValue().equals(order.getStatus())) {
                log.warn("已支付订单状态异常，恢复为paid状态: orderNo={}, paidAt={}", order.getOrderNo(), order.getPaidAt());
                order.setStatus(PaymentStatus.PAID.getValue());
                subscriptionOrderMapper.update(order);
                updatePaymentRecordStatus(order.getOrderNo(), PaymentStatus.PAID.getValue(), null);
            }
            
            return order;
        } catch (Exception e) {
            log.error("同步订单状态异常: orderNo={}", order.getOrderNo(), e);
            return order;
        }
    }
    
    /**
     * 获取状态显示名称
     */
    private String getStatusDisplayName(String statusValue) {
        PaymentStatus status = PaymentStatus.fromValue(statusValue);
        return status != null ? status.getDescription() : statusValue;
    }
    
    @Override
    public int syncPendingOrdersStatus() {
        log.debug("开始批量同步处理中订单状态");
        
        try {
            // 1. 查询所有处理中的订单（非最终状态）
            List<SubscriptionOrderEntity> pendingOrders = findPendingOrders();
            
            if (pendingOrders.isEmpty()) {
                log.debug("没有需要同步状态的订单");
                return 0;
            }
            
            log.info("找到 {} 个需要同步状态的订单", pendingOrders.size());
            
            int syncCount = 0;
            
            // 2. 逐个同步订单状态
            for (SubscriptionOrderEntity order : pendingOrders) {
                try {
                    // 检查订单是否过期（超过30分钟未支付的订单）
                    if (isOrderExpired(order)) {
                        updateOrderToExpired(order);
                        syncCount++;
                        continue;
                    }
                    
                    // 同步微信状态
                    SubscriptionOrderEntity updatedOrder = syncOrderStatusFromWechat(order);
                    if (updatedOrder != null && !updatedOrder.getStatus().equals(order.getStatus())) {
                        syncCount++;
                    }
                    
                    // 避免频繁调用API，每次调用间隔100ms
                    Thread.sleep(100);
                    
                } catch (Exception e) {
                    log.warn("同步订单状态失败，将在下次任务中重试: orderNo={}, error={}", 
                            order.getOrderNo(), e.getMessage());
                }
            }
            
            log.info("批量状态同步完成，成功同步 {} 个订单", syncCount);
            return syncCount;
            
        } catch (Exception e) {
            log.error("批量状态同步异常", e);
            throw new RuntimeException("批量状态同步失败: " + e.getMessage(), e);
        }
    }
    
    /**
     * 查询所有处理中的订单
     */
    private List<SubscriptionOrderEntity> findPendingOrders() {
        // 查询所有非最终状态的订单
        QueryWrapper wrapper = QueryWrapper.create()
                .where(SubscriptionOrderEntity::getStatus).in(
                        PaymentStatus.WAITING.getValue(),
                        PaymentStatus.PENDING.getValue(),
                        PaymentStatus.REFUND_PROCESSING.getValue()
                )
                .and(SubscriptionOrderEntity::getCreateTime).ge(
                        LocalDateTime.now().minusDays(1)  // 只查询3天内的订单
                )
                .orderBy(SubscriptionOrderEntity::getCreateTime, true);  // 按创建时间升序
        
        return subscriptionOrderMapper.selectListByQuery(wrapper);
    }
    
    /**
     * 检查订单是否已过期
     */
    private boolean isOrderExpired(SubscriptionOrderEntity order) {
        if (order.getExpiredAt() == null) {
            return false;
        }
        
        // 如果订单已经支付成功，不应该被标记为过期
        if (order.getPaidAt() != null) {
            return false;
        }
        
        return LocalDateTime.now().isAfter(order.getExpiredAt());
    }
    
    /**
     * 将订单更新为过期状态
     */
    private void updateOrderToExpired(SubscriptionOrderEntity order) {
        log.info("订单已过期，更新状态: orderNo={}, expiredAt={}", 
                order.getOrderNo(), order.getExpiredAt());
        
        String oldStatus = order.getStatus();
        
        // 使用事务方式更新所有相关表
        selfProxy.updateAllRelatedTablesWithTransaction(order, PaymentStatus.EXPIRED.getValue(), null);
        
        // 发送状态变化事件
        sendPaymentStatusEvent(order.getOrderNo(), PaymentStatus.EXPIRED.getValue(), 
                             order.getAmount(), "wechat", "订单已过期");
        
        log.info("订单状态已更新为过期: orderNo={}, oldStatus={}", order.getOrderNo(), oldStatus);
    }
    
    /**
     * 更新支付记录状态
     */
    private void updatePaymentRecordStatus(String orderNo, String newStatus, WxPayOrderQueryV3Result wxResult) {
        try {
            // 1. 查询支付记录
            QueryWrapper wrapper = QueryWrapper.create()
                    .where(PaymentRecordEntity::getOrderNo).eq(orderNo);
            PaymentRecordEntity paymentRecord = paymentRecordMapper.selectOneByQuery(wrapper);
            
            if (paymentRecord == null) {
                log.warn("未找到订单对应的支付记录: orderNo={}", orderNo);
                return;
            }
            
            String oldPaymentStatus = paymentRecord.getStatus();
            String newPaymentStatus = mapOrderStatusToPaymentStatus(newStatus);
            
            // 2. 检查状态是否需要更新
            if (newPaymentStatus.equals(oldPaymentStatus)) {
                log.debug("支付记录状态无需更新: orderNo={}, status={}", orderNo, oldPaymentStatus);
                return;
            }
            
            log.info("更新支付记录状态: orderNo={}, oldStatus={}, newStatus={}", 
                    orderNo, oldPaymentStatus, newPaymentStatus);
            
            // 3. 更新支付记录状态
            paymentRecord.setStatus(newPaymentStatus);
            
            // 4. 如果是支付成功状态，更新相关信息
            if ("success".equals(newPaymentStatus) && wxResult != null) {
                if (paymentRecord.getPaidAt() == null) {
                    paymentRecord.setPaidAt(LocalDateTime.now());
                }
                if (paymentRecord.getThirdPartyTransactionNo() == null && wxResult.getTransactionId() != null) {
                    paymentRecord.setThirdPartyTransactionNo(wxResult.getTransactionId());
                }
            }
            
            // 5. 如果是失败状态，设置失败原因
            if ("failed".equals(newPaymentStatus)) {
                paymentRecord.setFailureReason("支付失败");
            }
            
            // 6. 更新数据库
            paymentRecordMapper.update(paymentRecord);
            
            log.info("支付记录状态更新完成: orderNo={}, oldStatus={}, newStatus={}", 
                    orderNo, oldPaymentStatus, newPaymentStatus);
            
        } catch (Exception e) {
            log.error("更新支付记录状态失败: orderNo={}, newStatus={}", orderNo, newStatus, e);
            // 不抛出异常，避免影响订单状态更新
        }
    }
    
    /**
     * 以事务方式更新订单相关的所有表，确保数据一致性
     */
    @Transactional(rollbackFor = Exception.class)
    public void  updateAllRelatedTablesWithTransaction(SubscriptionOrderEntity order, String newStatus, WxPayOrderQueryV3Result wxResult) {
        String oldStatus = order.getStatus();
        String orderNo = order.getOrderNo();
        
        log.info("开始事务更新订单相关表: orderNo={}, oldStatus={}, newStatus={}", orderNo, oldStatus, newStatus);
        
        try {
            // 1. 更新订单表
            order.setStatus(newStatus);
            subscriptionOrderMapper.update(order);
            log.debug("订单表更新完成: orderNo={}", orderNo);
            
            // 2. 更新支付记录表
            updatePaymentRecordStatus(orderNo, newStatus, wxResult);
            log.debug("支付记录表更新完成: orderNo={}", orderNo);
            
            // 3. 根据状态变化处理用户订阅
            handleUserSubscriptionStatusChange(order, oldStatus, newStatus);
            log.debug("用户订阅状态处理完成: orderNo={}", orderNo);
            
            // 4. 根据状态变化处理积分
            handleCreditStatusChange(order, oldStatus, newStatus);
            log.debug("积分状态处理完成: orderNo={}", orderNo);
            
            log.info("订单相关表事务更新成功: orderNo={}, oldStatus={}, newStatus={}", orderNo, oldStatus, newStatus);
            
        } catch (Exception e) {
            log.error("订单相关表事务更新失败，将回滚: orderNo={}, oldStatus={}, newStatus={}", orderNo, oldStatus, newStatus, e);
            throw e; // 重新抛出异常，触发事务回滚
        }
    }
    
    /**
     * 处理用户订阅状态变化
     */
    private void handleUserSubscriptionStatusChange(SubscriptionOrderEntity order, String oldStatus, String newStatus) {
        // 如果从非支付状态变为支付成功状态，需要激活订阅
        if (!PaymentStatus.PAID.getValue().equals(oldStatus) && PaymentStatus.PAID.getValue().equals(newStatus)) {
            log.info("订单状态变为已支付，需要激活用户订阅: orderNo={}", order.getOrderNo());
            try {
                // 这里调用激活订阅的逻辑（如果还没有激活的话）
                activateUserSubscriptionIfNeeded(order);
            } catch (Exception e) {
                log.error("激活用户订阅失败: orderNo={}", order.getOrderNo(), e);
                throw new RuntimeException("激活用户订阅失败: " + e.getMessage());
            }
        }
        
        // 如果从支付成功状态变为其他状态，可能需要取消订阅
        if (PaymentStatus.PAID.getValue().equals(oldStatus) && !PaymentStatus.PAID.getValue().equals(newStatus)) {
            log.warn("订单状态从已支付变为其他状态，可能需要处理订阅: orderNo={}, oldStatus={}, newStatus={}", 
                    order.getOrderNo(), oldStatus, newStatus);
            
            // 根据新状态决定是否需要取消订阅
            if (PaymentStatus.CANCELLED.getValue().equals(newStatus) || 
                PaymentStatus.FAILED.getValue().equals(newStatus) ||
                PaymentStatus.EXPIRED.getValue().equals(newStatus)) {
                
                log.info("订单被取消/失败/过期，需要取消相关订阅: orderNo={}", order.getOrderNo());
                // 这里可以实现取消订阅的逻辑
                // cancelUserSubscriptionIfExists(order);
            }
        }
    }
    
    /**
     * 处理积分状态变化
     */
    private void handleCreditStatusChange(SubscriptionOrderEntity order, String oldStatus, String newStatus) {
        // 如果从非支付状态变为支付成功状态，需要发放积分
        if (!PaymentStatus.PAID.getValue().equals(oldStatus) && PaymentStatus.PAID.getValue().equals(newStatus)) {
            log.info("订单状态变为已支付，需要发放积分: orderNo={}", order.getOrderNo());
            try {
                // 发放积分逻辑（如果还没有发放的话）
                grantCreditIfNeeded(order);
            } catch (Exception e) {
                log.error("发放积分失败: orderNo={}", order.getOrderNo(), e);
                throw new RuntimeException("发放积分失败: " + e.getMessage());
            }
        }
        
        // 如果从支付成功状态变为其他状态，可能需要回滚积分
        if (PaymentStatus.PAID.getValue().equals(oldStatus) && !PaymentStatus.PAID.getValue().equals(newStatus)) {
            log.warn("订单状态从已支付变为其他状态，可能需要回滚积分: orderNo={}, oldStatus={}, newStatus={}", 
                    order.getOrderNo(), oldStatus, newStatus);
            
            // 根据新状态决定是否需要回滚积分
            if (PaymentStatus.CANCELLED.getValue().equals(newStatus) || 
                PaymentStatus.FAILED.getValue().equals(newStatus) ||
                PaymentStatus.EXPIRED.getValue().equals(newStatus)) {
                
                log.info("订单被取消/失败/过期，需要回滚积分: orderNo={}", order.getOrderNo());
                // 这里可以实现积分回滚的逻辑
                // rollbackCreditIfExists(order);
            }
        }
    }
    
    /**
     * 检查并激活用户订阅（如果需要）
     */
    private void activateUserSubscriptionIfNeeded(SubscriptionOrderEntity order) {
        // 检查是否已经有对应的用户订阅记录
        QueryWrapper wrapper = QueryWrapper.create()
                .where(UserSubscriptionEntity::getPayOrderNo).eq(order.getOrderNo());
        UserSubscriptionEntity existingSubscription = userSubscriptionMapper.selectOneByQuery(wrapper);
        
        if (existingSubscription != null) {
            log.debug("用户订阅已存在，无需重复激活: orderNo={}, subscriptionId={}", 
                    order.getOrderNo(), existingSubscription.getId());
            return;
        }
        
        log.info("用户订阅不存在，需要创建新的订阅记录: orderNo={}", order.getOrderNo());
        // 这里可以调用现有的激活订阅方法
        // 但为了避免重复代码，这里先留空，实际应该调用现有的 activateUserSubscription 方法
    }
    
    /**
     * 检查并发放积分（如果需要）
     */
    private void grantCreditIfNeeded(SubscriptionOrderEntity order) {
        // 检查是否已经发放过积分
        // 可以通过查询 t_credit_transaction 表来判断
        // 这里先留空，实际应该调用现有的积分发放方法
        log.debug("检查积分发放状态: orderNo={}", order.getOrderNo());
    }
    
    /**
     * 将订单状态映射到支付记录状态
     */
    private String mapOrderStatusToPaymentStatus(String orderStatus) {
        switch (orderStatus) {
            case "waiting":
            case "pending":
                return "waiting";
            case "paid":
                return "success";
            case "failed":
                return "failed";
            case "cancelled":
            case "expired":
                return "cancelled";
            case "refund_processing":
            case "refund_success":
            case "refund_fail":
            case "refund_closed":
            case "refund_abnormal":
                return "success";  // 退款相关状态，支付记录仍为成功，退款信息在别的地方管理
            default:
                log.warn("未知的订单状态: {}, 默认映射为waiting", orderStatus);
                return "waiting";
        }
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