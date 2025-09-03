/**
 * 全局配置包
 * 
 * <h3>JacksonConfig - 解决前端精度丢失问题</h3>
 * 
 * <h4>问题描述：</h4>
 * <p>JavaScript的Number类型只能安全表示到2^53-1（约16位）的整数，
 * 而我们使用雪花算法生成的ID是64位Long类型（可能达到19位），
 * 会导致前端接收到的ID值精度丢失。</p>
 * 
 * <h4>解决方案：</h4>
 * <p>通过Jackson配置，将所有Long类型的字段序列化为String类型返回给前端。</p>
 * 
 * <h4>前后端约定：</h4>
 * <ul>
 * <li><b>后端->前端</b>：ID字段作为String返回，如：{"id": "1234567890123456789"}</li>
 * <li><b>前端->后端</b>：URL路径参数和请求体中的ID可以是String，Spring会自动转换为Long</li>
 * <li><b>API文档</b>：ID示例使用19位数字字符串，便于前端开发参考</li>
 * </ul>
 * 
 * <h4>示例：</h4>
 * <pre>
 * // ✅ 正确的前端处理方式
 * const userId = "1234567890123456789";  // 使用字符串
 * 
 * // ❌ 错误的前端处理方式
 * const userId = 1234567890123456789;    // Number类型会丢失精度
 * </pre>
 * 
 * <h4>影响范围：</h4>
 * <ul>
 * <li>所有响应DTO中的Long类型字段</li>
 * <li>BaseEntity中的id、createBy、updateBy字段</li>
 * <li>业务实体中的各种ID关联字段</li>
 * <li>分页响应中的total字段（记录总数）</li>
 * </ul>
 * 
 * @author System
 * @since 1.0.0
 */
package com.noah.superagent.config;
