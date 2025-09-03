package com.noah.superagent.killbill.service;

import com.noah.superagent.common.dto.killbill.CreateAccountRequest;
import com.noah.superagent.common.dto.killbill.KillBillAccountResponse;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Kill Bill 服务接口
 *
 * @author Noah
 * @since 1.0.0
 */
public interface KillBillService {

    /**
     * 创建账户并添加积分
     *
     * @param request 创建账户请求
     * @return 账户响应
     */
    KillBillAccountResponse createAccountWithCredit(CreateAccountRequest request);

    /**
     * 给账户添加积分
     *
     * @param accountId    账户ID
     * @param creditAmount 积分金额
     */
    void addCreditToAccount(UUID accountId, BigDecimal creditAmount);

    /**
     * 查询账户余额
     *
     * @param accountId 账户ID
     * @return 余额
     */
    BigDecimal getAccountBalance(UUID accountId);

    /**
     * 根据外部键查询账户
     *
     * @param externalKey 外部键
     * @return 账户响应
     */
    KillBillAccountResponse getAccountByExternalKey(String externalKey);

    /**
     * 消费积分
     *
     * @param accountId   账户ID
     * @param amount      消费金额
     * @param description 描述
     * @return 是否成功
     */
    boolean consumeCredit(UUID accountId, BigDecimal amount, String description);
}
