package com.noah.superagent.dao.mapper;

import com.mybatisflex.core.BaseMapper;
import com.mybatisflex.core.query.QueryWrapper;
import com.noah.superagent.common.enums.CreditTypeEnum;
import com.noah.superagent.dao.entity.CreditTypeConfigEntity;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

import static com.noah.superagent.dao.entity.table.CreditTypeConfigEntityTableDef.CREDIT_TYPE_CONFIG_ENTITY;

/**
 * 积分类型配置Mapper接口
 *
 * @author 任相鹏
 * @since 1.0.0
 */
@Mapper
public interface CreditTypeConfigMapper extends BaseMapper<CreditTypeConfigEntity> {

    /**
     * 根据类型代码查询积分类型配置
     */
    default CreditTypeConfigEntity selectByTypeCode(CreditTypeEnum typeCode) {
        return selectOneByQuery(QueryWrapper.create()
                .where(CREDIT_TYPE_CONFIG_ENTITY.TYPE_CODE.eq(typeCode))
                .and(CREDIT_TYPE_CONFIG_ENTITY.ENABLED.eq(true))
        );
    }

    /**
     * 查询所有启用的积分类型配置，按消费优先级排序
     */
    default List<CreditTypeConfigEntity> selectAllEnabled() {
        return selectListByQuery(QueryWrapper.create()
                .where(CREDIT_TYPE_CONFIG_ENTITY.ENABLED.eq(true))
                .orderBy(CREDIT_TYPE_CONFIG_ENTITY.CONSUME_PRIORITY.asc())
        );
    }

    /**
     * 查询所有启用的积分类型配置，按消费优先级排序（用于扣费）
     */
    default List<CreditTypeConfigEntity> selectEnabledForConsumption() {
        return selectListByQuery(QueryWrapper.create()
                .where(CREDIT_TYPE_CONFIG_ENTITY.ENABLED.eq(true))
                .orderBy(CREDIT_TYPE_CONFIG_ENTITY.CONSUME_PRIORITY.asc())
        );
    }

    /**
     * 查询有有效期的积分类型（用于过期清理）
     */
    default List<CreditTypeConfigEntity> selectTemporaryTypes() {
        return selectListByQuery(QueryWrapper.create()
                .where(CREDIT_TYPE_CONFIG_ENTITY.ENABLED.eq(true))
                .and(CREDIT_TYPE_CONFIG_ENTITY.VALIDITY_DAYS.gt(0))
        );
    }

    /**
     * 根据有效期查询积分类型
     */
    default List<CreditTypeConfigEntity> selectByValidityDays(Integer validityDays) {
        return selectListByQuery(QueryWrapper.create()
                .where(CREDIT_TYPE_CONFIG_ENTITY.ENABLED.eq(true))
                .and(CREDIT_TYPE_CONFIG_ENTITY.VALIDITY_DAYS.eq(validityDays))
        );
    }
}
