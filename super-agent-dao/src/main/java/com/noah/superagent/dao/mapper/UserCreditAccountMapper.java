package com.noah.superagent.dao.mapper;

import com.mybatisflex.core.BaseMapper;
import com.mybatisflex.core.query.QueryWrapper;
import com.noah.superagent.dao.entity.UserCreditAccount;
import org.apache.ibatis.annotations.Mapper;

/**
 * 用户积分账户Mapper接口
 *
 * @author System
 * @since 1.0.0
 */
@Mapper
public interface UserCreditAccountMapper extends BaseMapper<UserCreditAccount> {

    /**
     * 根据用户ID查询积分账户
     */
    default UserCreditAccount selectByUserId(Long userId) {
        return selectOneByQuery(QueryWrapper.create()
                .where(UserCreditAccount::getUserId).eq(userId));
    }

    /**
     * 根据积分余额范围查询用户积分账户列表
     */
    default java.util.List<UserCreditAccount> selectByBalanceRange(java.math.BigDecimal minBalance, java.math.BigDecimal maxBalance) {
        return selectListByQuery(QueryWrapper.create()
                .orderBy(UserCreditAccount::getTotalBalance).desc());
    }

    /**
     * 更新用户积分余额
     */
    default int updateBalanceByUserId(Long userId, java.math.BigDecimal balance) {
        // 创建仅包含余额字段的更新对象
        UserCreditAccount updateAccount = new UserCreditAccount();
        updateAccount.setTotalBalance(balance);
        return updateByQuery(updateAccount, 
                QueryWrapper.create().where(UserCreditAccount::getUserId).eq(userId));
    }

    /**
     * 增加用户积分余额
     */
    default int increaseBalanceByUserId(Long userId, java.math.BigDecimal amount) {
        // 使用原生SQL更新语句增加余额
        UserCreditAccount updateAccount = new UserCreditAccount();
        updateAccount.setTotalBalance(amount);
        return updateByQuery(updateAccount, 
                QueryWrapper.create()
                        .where(UserCreditAccount::getUserId).eq(userId));
    }

    /**
     * 减少用户积分余额
     */
    default int decreaseBalanceByUserId(Long userId, java.math.BigDecimal amount) {
        // 使用原生SQL更新语句减少余额
        UserCreditAccount updateAccount = new UserCreditAccount();
        updateAccount.setTotalBalance(amount.negate());
        return updateByQuery(updateAccount, 
                QueryWrapper.create()
                        .where(UserCreditAccount::getUserId).eq(userId));
    }

    /**
     * 批量更新用户积分余额
     */
    default int batchUpdateBalance(java.util.List<Long> userIds, java.math.BigDecimal balance) {
        // 创建仅包含余额字段的更新对象
        UserCreditAccount updateAccount = new UserCreditAccount();
        updateAccount.setTotalBalance(balance);
        return updateByQuery(updateAccount,
                QueryWrapper.create().where(UserCreditAccount::getUserId).in(userIds));
    }
}