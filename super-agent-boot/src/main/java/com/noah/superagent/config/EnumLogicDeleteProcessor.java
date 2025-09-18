package com.noah.superagent.config;

import com.mybatisflex.core.dialect.IDialect;
import com.mybatisflex.core.logicdelete.LogicDeleteProcessor;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.core.table.TableInfo;
import com.noah.superagent.common.enums.DeletedEnum;

/**
 * 枚举逻辑删除处理器
 * 支持 DeletedEnum 枚举类型的逻辑删除字段处理
 * 
 * 按照官方 LogicDeleteProcessor 接口实现的自定义逻辑删除处理功能
 *
 * @author 任相鹏
 * @since 1.0.0
 */
public class EnumLogicDeleteProcessor implements LogicDeleteProcessor {

    @Override
    public String buildLogicNormalCondition(String logicColumn, TableInfo tableInfo, IDialect dialect) {
        // 构建查询正常数据的条件：deleted = 0 (NOT_DELETED)
        return dialect.wrap(logicColumn) + " = " + DeletedEnum.NOT_DELETED.getCode();
    }

    @Override
    public String buildLogicDeletedSet(String logicColumn, TableInfo tableInfo, IDialect dialect) {
        // 构建删除数据时的内容：set deleted = 1 (DELETED)
        return dialect.wrap(logicColumn) + " = " + DeletedEnum.DELETED.getCode();
    }

    @Override
    public void buildQueryCondition(QueryWrapper queryWrapper, TableInfo tableInfo, String joinTableAlias) {
        // 构建通过 QueryWrapper 查询数据时的条件
        String logicDeleteColumn = tableInfo.getLogicDeleteColumn();
        if (logicDeleteColumn != null) {
            String column = joinTableAlias != null ? 
                joinTableAlias + "." + logicDeleteColumn : logicDeleteColumn;
            queryWrapper.and(column + " = " + DeletedEnum.NOT_DELETED.getCode());
        }
    }

    @Override
    public Object getLogicNormalValue() {
        // 返回正常记录值：0 (NOT_DELETED)  
        return DeletedEnum.NOT_DELETED.getCode();
    }

    @Override
    public Object getLogicDeletedValue() {
        // 返回逻辑删除值：1 (DELETED)
        return DeletedEnum.DELETED.getCode();
    }
}
