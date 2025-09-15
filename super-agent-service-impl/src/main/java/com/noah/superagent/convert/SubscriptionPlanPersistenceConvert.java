package com.noah.superagent.convert;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.noah.superagent.dao.entity.SubscriptionPlanEntity;
import com.noah.superagent.model.PlanFeatureDTO;
import com.noah.superagent.model.SubscriptionPlanDTO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Collections;
import java.util.List;

/**
 * 订阅套餐持久化层转换器
 * 负责 DTO <-> Entity 转换
 *
 * @author AI Assistant
 * @since 1.0.0
 */
@Mapper(componentModel = "spring")
public abstract class SubscriptionPlanPersistenceConvert implements BasePersistenceConvert<
        SubscriptionPlanDTO,        // 数据传输对象类型
        SubscriptionPlanEntity      // 实体类型
> {

    @Autowired
    private ObjectMapper objectMapper;

    @Override
    @Mapping(target = "features", expression = "java(parseFeatures(entity.getFeatures()))")
    public abstract SubscriptionPlanDTO fromEntity(SubscriptionPlanEntity entity);

    @Override  
    @Mapping(target = "features", expression = "java(stringifyFeatures(dataTransferObject.getFeatures()))")
    public abstract SubscriptionPlanEntity toEntity(SubscriptionPlanDTO dataTransferObject);

    /**
     * 将JSON字符串解析为Feature对象列表
     */
    protected List<PlanFeatureDTO> parseFeatures(String featuresJson) {
        if (featuresJson == null || featuresJson.trim().isEmpty()) {
            return Collections.emptyList();
        }
        
        try {
            return objectMapper.readValue(featuresJson, new TypeReference<List<PlanFeatureDTO>>() {});
        } catch (Exception e) {
            return Collections.emptyList();
        }
    }

    /**
     * 将Feature对象列表转换为JSON字符串
     */
    protected String stringifyFeatures(List<PlanFeatureDTO> features) {
        if (features == null || features.isEmpty()) {
            return "[]";
        }
        
        try {
            return objectMapper.writeValueAsString(features);
        } catch (Exception e) {
            return "[]";
        }
    }
}
