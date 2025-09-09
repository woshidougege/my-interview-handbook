package com.noah.superagent.service.impl;

import com.noah.superagent.common.enums.CreditTypeEnum;
import com.noah.superagent.dao.entity.CreditTypeConfigEntity;
import com.noah.superagent.dao.mapper.CreditTypeConfigMapper;
import com.noah.superagent.service.CreditTypeConfigService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 积分类型配置服务实现
 * 
 * 管理积分类型的配置信息，包括有效期、优先级等
 *
 * @author Noah
 * @since 1.0.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CreditTypeConfigServiceImpl implements CreditTypeConfigService {

    private final CreditTypeConfigMapper creditTypeConfigMapper;

    @Override
    @Cacheable(value = "credit_type_config", key = "#typeCode")
    public CreditTypeConfigEntity getByTypeCode(CreditTypeEnum typeCode) {
        log.debug("查询积分类型配置 - typeCode: {}", typeCode);
        return creditTypeConfigMapper.selectByTypeCode(typeCode);
    }

    @Override
    @Cacheable(value = "credit_type_config", key = "'all_enabled'")
    public List<CreditTypeConfigEntity> getAllEnabled() {
        log.debug("查询所有启用的积分类型配置");
        return creditTypeConfigMapper.selectAllEnabled();
    }

    @Override
    @Cacheable(value = "credit_type_config", key = "'consumption_types'")
    public List<CreditTypeConfigEntity> getTypesForConsumption() {
        log.debug("查询用于扣费的积分类型配置");
        return creditTypeConfigMapper.selectEnabledForConsumption();
    }

    @Override
    @Cacheable(value = "credit_type_config", key = "'temporary_types'")
    public List<CreditTypeConfigEntity> getTemporaryTypes() {
        log.debug("查询有有效期的积分类型");
        return creditTypeConfigMapper.selectTemporaryTypes();
    }

    @Override
    @Cacheable(value = "credit_type_config", key = "'validity_' + #validityDays")
    public List<CreditTypeConfigEntity> getByValidityDays(Integer validityDays) {
        log.debug("根据有效期查询积分类型 - validityDays: {}", validityDays);
        return creditTypeConfigMapper.selectByValidityDays(validityDays);
    }

    @Override
    public boolean isTypeEnabled(CreditTypeEnum typeCode) {
        CreditTypeConfigEntity config = getByTypeCode(typeCode);
        return config != null && Boolean.TRUE.equals(config.getEnabled());
    }

    @Override
    public Integer getValidityDays(CreditTypeEnum typeCode) {
        CreditTypeConfigEntity config = getByTypeCode(typeCode);
        return config != null ? config.getValidityDays() : null;
    }

    @Override
    public Integer getConsumePriority(CreditTypeEnum typeCode) {
        CreditTypeConfigEntity config = getByTypeCode(typeCode);
        return config != null ? config.getConsumePriority() : null;
    }

    @Override
    @CacheEvict(value = "credit_type_config", allEntries = true)
    public void refreshCache() {
        log.info("刷新积分类型配置缓存");
    }

    /**
     * 获取积分类型配置映射（内部使用）
     */
    public Map<CreditTypeEnum, CreditTypeConfigEntity> getConfigMap() {
        return getAllEnabled().stream()
                .collect(Collectors.toMap(
                        CreditTypeConfigEntity::getTypeCode,
                        Function.identity()
                ));
    }

    /**
     * 批量获取积分类型配置
     */
    public Map<CreditTypeEnum, CreditTypeConfigEntity> getBatchByTypeCodes(List<CreditTypeEnum> typeCodes) {
        return typeCodes.stream()
                .collect(Collectors.toMap(
                        Function.identity(),
                        this::getByTypeCode
                ))
                .entrySet().stream()
                .filter(entry -> entry.getValue() != null)
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        Map.Entry::getValue
                ));
    }
}
