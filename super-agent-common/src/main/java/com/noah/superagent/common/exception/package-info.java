/**
 * 异常处理包
 * 
 * <h3>使用说明：</h3>
 * 
 * <h4>1. 业务异常抛出示例：</h4>
 * <pre>
 * // 简单抛出异常
 * throw new BusinessException("用户不存在");
 * 
 * // 使用枚举抛出异常
 * throw new BusinessException(ResponseCodeEnum.USER_NOT_FOUND);
 * 
 * // 条件抛出异常
 * BusinessException.throwIf(user == null, "用户不存在");
 * BusinessException.throwIf(user == null, ResponseCodeEnum.USER_NOT_FOUND);
 * </pre>
 * 
 * <h4>2. 在Service层使用：</h4>
 * <pre>
 * {@code @Service}
 * public class UserServiceImpl implements UserService {
 *     
 *     public UserResponse getUserById(Long id) {
 *         User user = userMapper.selectOneById(id);
 *         // 简洁的异常处理
 *         BusinessException.throwIf(user == null, ResponseCodeEnum.USER_NOT_FOUND);
 *         
 *         return userConverter.toResponse(user);
 *     }
 *     
 *     public UserResponse createUser(UserCreateRequest request) {
 *         // 检查手机号是否存在
 *         boolean exists = userMapper.existsByPhone(request.getPhone());
 *         BusinessException.throwIf(exists, ResponseCodeEnum.PHONE_EXISTS);
 *         
 *         // 创建用户逻辑...
 *     }
 * }
 * </pre>
 * 
 * <h4>3. Controller层自动处理：</h4>
 * Controller层不需要try-catch，全局异常处理器会自动处理BusinessException并返回统一格式的ApiResponse。
 * 
 * @author System
 * @since 1.0.0
 */
package com.noah.superagent.common.exception;
