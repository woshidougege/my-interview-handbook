package com.noah.superagent.config;

import com.mybatisflex.core.FlexGlobalConfig;
import com.mybatisflex.core.audit.AuditManager;
import com.mybatisflex.core.logicdelete.LogicDeleteManager;
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
        // 使用自定义枚举逻辑删除处理器：按照官方文档推荐的接口实现
        LogicDeleteManager.setProcessor(new EnumLogicDeleteProcessor());
        
        // 可选：全局配置逻辑删除字段名（如果所有表都使用相同的字段名）
        // globalConfig.setLogicDeleteColumn("deleted");
        
        // 雪花算法ID生成器已内置，无需额外配置
        // 直接在实体类使用: @Id(keyType = KeyType.Generator, value = "snowFlakeId")
    }
}
