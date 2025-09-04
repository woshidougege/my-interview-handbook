package com.noah.superagent.config;

import com.mybatisflex.core.FlexGlobalConfig;
import com.mybatisflex.core.audit.AuditManager;
import com.mybatisflex.core.logicdelete.LogicDeleteManager;
import com.mybatisflex.core.logicdelete.impl.IntegerLogicDeleteProcessor;
import com.mybatisflex.spring.boot.MyBatisFlexCustomizer;
import org.springframework.context.annotation.Configuration;

/**
 * MyBatis Flex 配置
 *
 * @author 任相鹏
 * @since 1.0.0
 */
@Configuration
public class MyBatisFlexConfig implements MyBatisFlexCustomizer {

    @Override
    public void customize(FlexGlobalConfig globalConfig) {
        // 开启审计功能（可选）
        AuditManager.setAuditEnable(true);
        
        // 配置逻辑删除处理器
        // 使用Integer类型的逻辑删除处理器：0表示正常，1表示已删除
        LogicDeleteManager.setProcessor(new IntegerLogicDeleteProcessor());
        
        // 可选：全局配置逻辑删除字段名（如果所有表都使用相同的字段名）
        // globalConfig.setLogicDeleteColumn("deleted");
        
        // 雪花算法ID生成器已内置，无需额外配置
        // 直接在实体类使用: @Id(keyType = KeyType.Generator, value = "snowFlakeId")
    }
}
