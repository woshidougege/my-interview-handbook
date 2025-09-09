package com.noah.superagent.service;

import com.noah.superagent.common.enums.CreditTypeEnum;
import com.noah.superagent.dao.entity.CreditTypeConfigEntity;

import java.util.List;

/**
 * 积分类型配置服务接口
 * 
 * 管理积分类型的配置信息，包括有效期、优先级等
 *
 * @author Noah
 * @since 1.0.0
 */
public interface CreditTypeConfigService {

    /**
     * 根据类型代码获取积分类型配置
     *
     * @param typeCode 积分类型代码
     * @return 积分类型配置，不存在时返回null
     */
    CreditTypeConfigEntity getByTypeCode(CreditTypeEnum typeCode);

    /**
     * 获取所有启用的积分类型配置，按消费优先级排序
     *
     * @return 积分类型配置列表
     */
    List<CreditTypeConfigEntity> getAllEnabled();

    /**
     * 获取用于扣费的积分类型配置，按消费优先级排序
     *
     * @return 积分类型配置列表
     */
    List<CreditTypeConfigEntity> getTypesForConsumption();

    /**
     * 获取有有效期的积分类型（用于过期清理）
     *
     * @return 积分类型配置列表
     */
    List<CreditTypeConfigEntity> getTemporaryTypes();

    /**
     * 根据有效期获取积分类型
     *
     * @param validityDays 有效期天数
     * @return 积分类型配置列表
     */
    List<CreditTypeConfigEntity> getByValidityDays(Integer validityDays);

    /**
     * 检查积分类型是否存在且启用
     *
     * @param typeCode 积分类型代码
     * @return 是否存在且启用
     */
    boolean isTypeEnabled(CreditTypeEnum typeCode);

    /**
     * 获取积分类型的有效期
     *
     * @param typeCode 积分类型代码
     * @return 有效期天数，永久积分返回0，不存在的类型返回null
     */
    Integer getValidityDays(CreditTypeEnum typeCode);

    /**
     * 获取积分类型的消费优先级
     *
     * @param typeCode 积分类型代码
     * @return 消费优先级，不存在的类型返回null
     */
    Integer getConsumePriority(CreditTypeEnum typeCode);

    /**
     * 刷新积分类型配置缓存
     */
    void refreshCache();
}
