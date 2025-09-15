package com.noah.superagent.convert;

import com.noah.superagent.common.dto.response.SubscriptionPlanResponse;
import com.noah.superagent.model.SubscriptionPlanDTO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

/**
 * 订阅套餐Web层转换器
 * 负责 DTO <-> Response 转换
 *
 * @author AI Assistant
 * @since 1.0.0
 */
@Mapper(componentModel = "spring")
public interface SubscriptionPlanWebConvert {

    /**
     * DTO转换为Response
     */
    @Mapping(target = "enabled", expression = "java(dto.getEnabled() == com.noah.superagent.common.enums.EnabledEnum.ENABLED)")
    SubscriptionPlanResponse toResponse(SubscriptionPlanDTO dto);

    /**
     * DTO列表转换为Response列表
     */
    List<SubscriptionPlanResponse> toResponseList(List<SubscriptionPlanDTO> dtos);
}
