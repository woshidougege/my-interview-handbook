package com.noah.superagent.service.impl;

import com.noah.superagent.common.enums.EnabledEnum;
import com.noah.superagent.convert.SubscriptionPlanPersistenceConvert;
import com.noah.superagent.dao.mapper.SubscriptionPlanMapper;
import com.noah.superagent.model.SubscriptionPlanDTO;
import com.noah.superagent.service.SubscriptionPlanService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 订阅套餐服务实现类
 *
 * @author AI Assistant
 * @since 1.0.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SubscriptionPlanServiceImpl implements SubscriptionPlanService {

    private final SubscriptionPlanMapper subscriptionPlanMapper;
    private final SubscriptionPlanPersistenceConvert convert;

    @Override
    public List<SubscriptionPlanDTO> getEnabledPlans() {
        log.info("获取启用的订阅套餐列表");
        
        return subscriptionPlanMapper.selectAll()
                .stream()
                .filter(plan -> EnabledEnum.ENABLED.equals(plan.getEnabled()))
                .map(convert::entityToDto)
                .collect(Collectors.toList());
    }
}
