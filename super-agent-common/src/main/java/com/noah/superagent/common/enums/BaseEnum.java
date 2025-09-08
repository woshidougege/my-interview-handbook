package com.noah.superagent.common.enums;

/**
 * 枚举基础接口
 * 为所有业务枚举提供统一的行为规范
 * 配合MyBatis-Flex的@EnumValue注解使用
 *
 * @author 任相鹏
 * @since 1.0.0
 */
public interface BaseEnum<T> {
    
    /**
     * 获取枚举的编码值
     * 用于数据库存储和JSON序列化
     * 注意：需要在实现类的code字段上添加@EnumValue和@JsonValue注解
     *
     * @return 编码值
     */
    T getCode();
    
    /**
     * 获取枚举的描述信息
     *
     * @return 描述信息
     */
    String getDesc();
    
    /**
     * 通用枚举转换工具方法
     * 用于@JsonCreator方法中
     *
     * @param enumClass 枚举类
     * @param code 编码值
     * @param <E> 枚举类型
     * @param <T> 编码类型
     * @return 对应的枚举实例，未找到返回null
     */
    static <E extends BaseEnum<T>, T> E getByCode(Class<E> enumClass, T code) {
        if (code == null) {
            return null;
        }
        
        E[] enumConstants = enumClass.getEnumConstants();
        if (enumConstants == null) {
            return null;
        }
        
        for (E enumConstant : enumConstants) {
            if (enumConstant.getCode().equals(code)) {
                return enumConstant;
            }
        }
        return null;
    }
}
