package com.noah.superagent.dao.mapper;

import com.mybatisflex.core.BaseMapper;
import com.mybatisflex.core.query.QueryWrapper;
import com.noah.superagent.dao.entity.TokenUsageRecordEntity;
import org.apache.ibatis.annotations.Mapper;

import static com.noah.superagent.dao.entity.table.TokenUsageRecordEntityTableDef.TOKEN_USAGE_RECORD_ENTITY;

/**
 * Token使用记录Mapper - 简化版
 *
 * @author Noah
 * @since 1.0.0
 */
@Mapper
public interface TokenUsageRecordMapper extends BaseMapper<TokenUsageRecordEntity> {

    /**
     * 根据请求ID查询记录（用于幂等性检查）
     */
    default TokenUsageRecordEntity findByRequestId(String requestId) {
        return selectOneByQuery(QueryWrapper.create()
                .select()
                .where(TOKEN_USAGE_RECORD_ENTITY.REQUEST_ID.eq(requestId))
                .and(TOKEN_USAGE_RECORD_ENTITY.DELETED.eq(0))
        );
    }
}
