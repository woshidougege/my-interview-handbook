package com.noah.superagent.service.impl;

import com.mybatisflex.core.query.QueryWrapper;
import com.noah.superagent.common.enums.PlanCodeEnum;
import com.noah.superagent.common.enums.SubscriptionStatusEnum;
import com.noah.superagent.convert.UserSubscriptionPersistenceConvert;
import com.noah.superagent.dao.entity.UserSubscriptionEntity;
import com.noah.superagent.dao.mapper.UserSubscriptionMapper;
import com.noah.superagent.model.SubscriptionPlanDTO;
import com.noah.superagent.model.UserSubscriptionDTO;
import com.noah.superagent.service.SubscriptionPlanService;
import com.noah.superagent.service.UserSubscriptionService;
import com.noah.superagent.common.event.SubscriptionActivatedEvent;
import com.noah.superagent.common.event.SubscriptionExtendedEvent;
import org.springframework.context.ApplicationEventPublisher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 用户订阅服务实现类
 * 支持升级时延长下级套餐的智能订阅管理
 *
 * @author 任相鹏
 * @since 1.0.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserSubscriptionServiceImpl implements UserSubscriptionService {

    private final UserSubscriptionMapper userSubscriptionMapper;
    private final UserSubscriptionPersistenceConvert convert;
    private final SubscriptionPlanService subscriptionPlanService;
    private final ApplicationEventPublisher eventPublisher;

    @Override
    public UserSubscriptionDTO getCurrentActiveSubscription(Long userId) {
        log.info("查询用户当前有效订阅 - userId: {}", userId);

        // 获取所有有效订阅，返回等级最高的
        List<UserSubscriptionDTO> activeSubscriptions = getAllActiveSubscriptions(userId);
        return activeSubscriptions.isEmpty() ? null : activeSubscriptions.get(0);
    }

    @Override
    public List<UserSubscriptionDTO> getAllActiveSubscriptions(Long userId) {
        log.info("查询用户所有有效订阅 - userId: {}", userId);

        LocalDateTime now = LocalDateTime.now();
        
        QueryWrapper queryWrapper = QueryWrapper.create()
                .where(UserSubscriptionEntity::getUserId).eq(userId)
                .and(UserSubscriptionEntity::getStatus).eq(SubscriptionStatusEnum.ACTIVE)
                .and(UserSubscriptionEntity::getEndTime).gt(now)
                .orderBy(UserSubscriptionEntity::getCreateTime, false);

        List<UserSubscriptionEntity> entities = userSubscriptionMapper.selectListByQuery(queryWrapper);
        
        return entities.stream()
                .map(convert::fromEntity)
                .sorted((a, b) -> {
                    // 按套餐等级排序：高级版 > 基础版 > 免费版
                    int levelA = getPlanLevel(a.getPlanId());
                    int levelB = getPlanLevel(b.getPlanId());
                    return Integer.compare(levelB, levelA); // 降序
                })
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public UserSubscriptionDTO activateSubscriptionWithUpgradeLogic(Long userId, Long planId, String billingCycle, 
                                                                   String orderNo, BigDecimal paidAmount) {
        log.info("智能激活用户订阅 - userId: {}, planId: {}, billingCycle: {}", userId, planId, billingCycle);

        // 1. 获取要激活的套餐信息
        SubscriptionPlanDTO targetPlan = subscriptionPlanService.getPlanById(planId);
        if (targetPlan == null) {
            throw new RuntimeException("套餐不存在: " + planId);
        }

        // 2. 检查是否为积分购买套餐（不需要激活订阅）
        if (targetPlan.getPlanCode() == PlanCodeEnum.CREDIT_PACK) {
            log.info("积分购买套餐不需要激活订阅");
            return null;
        }

        // 3. 获取用户当前所有有效订阅
        List<UserSubscriptionDTO> currentSubscriptions = getAllActiveSubscriptions(userId);
        
        // 4. 计算新订阅的有效期
        LocalDateTime startTime = LocalDateTime.now();
        LocalDateTime endTime = calculateSubscriptionEndTime(startTime, billingCycle);

        // 5. 处理升级逻辑
        if (targetPlan.getPlanCode() == PlanCodeEnum.PREMIUM && hasBasicPlan(currentSubscriptions)) {
            // 高级版升级：延长基础版有效期
            extendBasicPlanSubscription(userId, billingCycle);
            log.info("用户升级高级版，基础版已延期");
        }

        // 6. 创建新的订阅记录
        UserSubscriptionEntity newSubscription = new UserSubscriptionEntity();
        newSubscription.setUserId(userId);
        newSubscription.setPlanId(planId);
        newSubscription.setStartTime(startTime);
        newSubscription.setEndTime(endTime);
        newSubscription.setPaidAmount(paidAmount);
        newSubscription.setStatus(SubscriptionStatusEnum.ACTIVE);
        newSubscription.setPayOrderNo(orderNo);
        newSubscription.setRemark("套餐激活 - " + targetPlan.getPlanName());

        // 7. 设置积分数量（从配置获取）
        newSubscription.setCreditAmount(targetPlan.getCreditsAmount() != null 
            ? new BigDecimal(targetPlan.getCreditsAmount()) : BigDecimal.ZERO);

        userSubscriptionMapper.insert(newSubscription);

        log.info("新订阅创建成功 - subscriptionId: {}, planName: {}, endTime: {}", 
                newSubscription.getId(), targetPlan.getPlanName(), endTime);

        // 发布订阅激活事件，触发到期任务安排
        try {
            SubscriptionActivatedEvent event = new SubscriptionActivatedEvent(
                this,
                newSubscription.getId(),
                userId,
                planId,
                targetPlan.getPlanName(),
                endTime
            );
            eventPublisher.publishEvent(event);
            log.info("订阅激活事件已发布 - subscriptionId: {}", newSubscription.getId());
        } catch (Exception e) {
            log.error("发布订阅激活事件失败 - subscriptionId: {}, error: {}", 
                    newSubscription.getId(), e.getMessage(), e);
            // 事件发布失败不影响主流程
        }

        return convert.fromEntity(newSubscription);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int processSubscriptionExpirations() {
        log.info("开始处理套餐到期和降级逻辑");

        LocalDateTime now = LocalDateTime.now();
        int processedCount = 0;

        try {
            // 查询所有即将在未来1小时内到期的有效订阅
            QueryWrapper queryWrapper = QueryWrapper.create()
                    .where(UserSubscriptionEntity::getStatus).eq(SubscriptionStatusEnum.ACTIVE)
                    .and(UserSubscriptionEntity::getEndTime).le(now.plusHours(1))
                    .and(UserSubscriptionEntity::getEndTime).gt(now.minusHours(1));

            List<UserSubscriptionEntity> expiredSubscriptions = userSubscriptionMapper.selectListByQuery(queryWrapper);

            for (UserSubscriptionEntity subscription : expiredSubscriptions) {
                if (subscription.getEndTime().isBefore(now)) {
                    processExpiredSubscription(subscription);
                    processedCount++;
                }
            }

            log.info("套餐到期处理完成，处理了 {} 个订阅", processedCount);
            return processedCount;

        } catch (Exception e) {
            log.error("处理套餐到期异常", e);
            throw new RuntimeException("处理套餐到期失败: " + e.getMessage());
        }
    }

    /**
     * 计算订阅结束时间
     */
    private LocalDateTime calculateSubscriptionEndTime(LocalDateTime startTime, String billingCycle) {
        if ("yearly".equals(billingCycle)) {
            return startTime.plusDays(365); // 年付365天
        } else {
            return startTime.plusDays(30);  // 月付30天
        }
    }

    /**
     * 检查用户是否有基础版订阅
     */
    private boolean hasBasicPlan(List<UserSubscriptionDTO> subscriptions) {
        return subscriptions.stream()
                .anyMatch(sub -> {
                    SubscriptionPlanDTO plan = subscriptionPlanService.getPlanById(sub.getPlanId());
                    return plan != null && plan.getPlanCode() == PlanCodeEnum.BASIC;
                });
    }

    /**
     * 延长基础版订阅有效期
     */
    private void extendBasicPlanSubscription(Long userId, String billingCycle) {
        QueryWrapper queryWrapper = QueryWrapper.create()
                .where(UserSubscriptionEntity::getUserId).eq(userId)
                .and(UserSubscriptionEntity::getStatus).eq(SubscriptionStatusEnum.ACTIVE)
                .and(UserSubscriptionEntity::getPlanId).eq(2L); // 基础版ID为2

        List<UserSubscriptionEntity> basicSubscriptions = userSubscriptionMapper.selectListByQuery(queryWrapper);

        for (UserSubscriptionEntity basicSub : basicSubscriptions) {
            if (basicSub.getEndTime().isAfter(LocalDateTime.now())) {
                // 根据购买的高级版周期延长基础版
                LocalDateTime extensionTime = "yearly".equals(billingCycle) 
                    ? basicSub.getEndTime().plusYears(1) 
                    : basicSub.getEndTime().plusMonths(1);

                basicSub.setEndTime(extensionTime);
                basicSub.setRemark(basicSub.getRemark() + " [因升级高级版而延期]");
                userSubscriptionMapper.update(basicSub);

                // 发布订阅延期事件，重新安排到期任务
                try {
                    SubscriptionPlanDTO basicPlan = subscriptionPlanService.getPlanById(basicSub.getPlanId());
                    if (basicPlan != null) {
                        SubscriptionExtendedEvent event = new SubscriptionExtendedEvent(
                            this,
                            basicSub.getId(),
                            userId,
                            basicSub.getPlanId(),
                            basicPlan.getPlanName(),
                            extensionTime
                        );
                        eventPublisher.publishEvent(event);
                        log.info("订阅延期事件已发布 - subscriptionId: {}, newEndTime: {}", 
                                basicSub.getId(), extensionTime);
                    }
                } catch (Exception e) {
                    log.error("发布订阅延期事件失败 - subscriptionId: {}, error: {}", 
                            basicSub.getId(), e.getMessage(), e);
                    // 事件发布失败不影响主流程
                }

                log.info("基础版订阅已延期 - userId: {}, newEndTime: {}", userId, extensionTime);
            }
        }
    }

    /**
     * 处理已过期的订阅
     */
    private void processExpiredSubscription(UserSubscriptionEntity expiredSubscription) {
        log.info("处理过期订阅 - userId: {}, planId: {}, endTime: {}", 
                expiredSubscription.getUserId(), expiredSubscription.getPlanId(), expiredSubscription.getEndTime());

        // 标记为过期
        expiredSubscription.setStatus(SubscriptionStatusEnum.EXPIRED);
        expiredSubscription.setRemark(expiredSubscription.getRemark() + " [已到期]");
        userSubscriptionMapper.update(expiredSubscription);

        // 获取套餐信息
        SubscriptionPlanDTO plan = subscriptionPlanService.getPlanById(expiredSubscription.getPlanId());
        if (plan == null) return;

        // 处理降级逻辑
        if (plan.getPlanCode() == PlanCodeEnum.PREMIUM) {
            // 高级版到期，检查是否有基础版可以降级到
            handlePremiumPlanExpiration(expiredSubscription.getUserId());
        } else if (plan.getPlanCode() == PlanCodeEnum.BASIC) {
            // 基础版到期，自动回到免费版（无需特殊处理，系统默认就是免费版）
            log.info("基础版已到期，用户回到免费版 - userId: {}", expiredSubscription.getUserId());
        }
    }

    /**
     * 处理高级版过期后的降级逻辑
     */
    private void handlePremiumPlanExpiration(Long userId) {
        // 查找用户是否有仍然有效的基础版订阅
        List<UserSubscriptionDTO> activeBasicSubs = getAllActiveSubscriptions(userId)
                .stream()
                .filter(sub -> {
                    SubscriptionPlanDTO plan = subscriptionPlanService.getPlanById(sub.getPlanId());
                    return plan != null && plan.getPlanCode() == PlanCodeEnum.BASIC;
                })
                .collect(Collectors.toList());

        if (!activeBasicSubs.isEmpty()) {
            log.info("高级版到期，用户降级到基础版 - userId: {}, 基础版到期时间: {}", 
                    userId, activeBasicSubs.get(0).getEndTime());
        } else {
            log.info("高级版到期，用户回到免费版 - userId: {}", userId);
        }
    }

    /**
     * 获取套餐等级（用于排序）
     */
    private int getPlanLevel(Long planId) {
        SubscriptionPlanDTO plan = subscriptionPlanService.getPlanById(planId);
        if (plan == null || plan.getPlanCode() == null) return 0;
        
        return plan.getPlanCode().getLevel();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean processSpecificSubscriptionExpiration(Long subscriptionId) {
        log.info("处理特定订阅到期 - subscriptionId: {}", subscriptionId);

        try {
            // 1. 查询订阅记录
            UserSubscriptionEntity subscription = userSubscriptionMapper.selectOneById(subscriptionId);
            if (subscription == null) {
                log.warn("订阅记录不存在 - subscriptionId: {}", subscriptionId);
                return false;
            }

            // 2. 检查订阅是否确实已到期
            LocalDateTime now = LocalDateTime.now();
            if (!subscription.getEndTime().isBefore(now)) {
                log.warn("订阅尚未到期，跳过处理 - subscriptionId: {}, endTime: {}, now: {}", 
                        subscriptionId, subscription.getEndTime(), now);
                return false;
            }

            // 3. 检查订阅状态是否为有效状态
            if (!SubscriptionStatusEnum.ACTIVE.equals(subscription.getStatus())) {
                log.warn("订阅状态不是有效状态，跳过处理 - subscriptionId: {}, status: {}", 
                        subscriptionId, subscription.getStatus());
                return false;
            }

            // 4. 处理订阅到期
            processExpiredSubscription(subscription);
            
            log.info("特定订阅到期处理完成 - subscriptionId: {}", subscriptionId);
            return true;

        } catch (Exception e) {
            log.error("处理特定订阅到期异常 - subscriptionId: {}", subscriptionId, e);
            throw new RuntimeException("处理订阅到期失败: " + e.getMessage(), e);
        }
    }

}