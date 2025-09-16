package com.noah.superagent.service.impl;

import com.noah.superagent.common.enums.SubscriptionStatusEnum;
import com.noah.superagent.convert.UserSubscriptionPersistenceConvert;
import com.noah.superagent.dao.mapper.UserSubscriptionMapper;
import com.noah.superagent.model.UserSubscriptionDTO;
import com.noah.superagent.service.UserSubscriptionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * 用户订阅服务实现类
 * 对应数据库表：t_user_subscription
 *
 * @author AI Assistant
 * @since 1.0.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserSubscriptionServiceImpl implements UserSubscriptionService {

    private final UserSubscriptionMapper userSubscriptionMapper;
    private final UserSubscriptionPersistenceConvert convert;

    @Override
    public UserSubscriptionDTO getCurrentActiveSubscription(Long userId) {
        log.info("查询用户当前有效订阅 - userId: {}", userId);

        return userSubscriptionMapper.selectAll()
                .stream()
                .filter(sub -> sub.getUserId().equals(userId)
                        && SubscriptionStatusEnum.ACTIVE.equals(sub.getStatus())
                        && sub.getEndTime().isAfter(LocalDateTime.now()))
                .findFirst()
                .map(convert::fromEntity)
                .orElse(null);
    }
}