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
 * throw new BusinessException(ResponseCodeEnum.CREDIT_ACCOUNT_NOT_FOUND);
 * 
 * // 条件抛出异常
 * BusinessException.throwIf(account == null, "积分账户不存在");
 * BusinessException.throwIf(account == null, ResponseCodeEnum.CREDIT_ACCOUNT_NOT_FOUND);
 * </pre>
 * 
 * <h4>2. 在Service层使用：</h4>
 * <pre>
 * {@code @Service}
 * public class CreditServiceImpl implements CreditService {
 *     
 *     public CreditResponse getCreditByUserId(Long userId) {
 *         CreditAccount account = creditMapper.selectByUserId(userId);
 *         // 简洁的异常处理
 *         BusinessException.throwIf(account == null, ResponseCodeEnum.CREDIT_ACCOUNT_NOT_FOUND);
 *         
 *         return creditConverter.toResponse(account);
 *     }
 *     
 *     public void consumeCredits(Long userId, BigDecimal amount) {
 *         // 检查积分是否充足
 *         boolean sufficient = hasEnoughCredits(userId, amount);
 *         BusinessException.throwIf(!sufficient, ResponseCodeEnum.INSUFFICIENT_CREDITS);
 *         
 *         // 扣除积分逻辑...
 *     }
 * }
 * </pre>
 * 
 * <h4>3. Controller层自动处理：</h4>
 * Controller层不需要try-catch，全局异常处理器会自动处理BusinessException并返回统一格式的ApiResponse。
 * 
 * @author 任相鹏
 * @since 1.0.0
 */
package com.noah.superagent.common.exception;
