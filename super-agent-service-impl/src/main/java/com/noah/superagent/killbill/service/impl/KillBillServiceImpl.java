package com.noah.superagent.killbill.service.impl;

import com.noah.superagent.common.dto.killbill.CreateAccountRequest;
import com.noah.superagent.common.dto.killbill.KillBillAccountResponse;
import com.noah.superagent.killbill.service.KillBillService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.killbill.billing.client.KillBillClientException;
import org.killbill.billing.client.KillBillHttpClient;
import org.killbill.billing.client.RequestOptions;
import org.killbill.billing.client.api.gen.AccountApi;
import org.killbill.billing.client.api.gen.InvoiceApi;
import org.killbill.billing.client.model.gen.Account;

import org.killbill.billing.client.model.gen.InvoiceItem;
import org.killbill.billing.client.model.InvoiceItems;
import org.killbill.billing.catalog.api.Currency;
import org.killbill.billing.util.api.AuditLevel;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

/**
 * Kill Bill 服务实现 - 按官方推荐方式
 *
 * @author Noah
 * @since 1.0.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class KillBillServiceImpl implements KillBillService {

    private final KillBillHttpClient killBillClient;

    /**
     * 创建账户并添加积分
     */
    public KillBillAccountResponse createAccountWithCredit(CreateAccountRequest request) {
        try {
            log.info("🚀 开始创建Kill Bill账户: {}", request.getName());

            // 1. 使用官方推荐的AccountApi创建账户
            AccountApi accountApi = new AccountApi(killBillClient);
            
            Account account = new Account();
            account.setName(request.getName());
            account.setEmail(request.getEmail());
            account.setExternalKey(request.getExternalKey());
            account.setCurrency(Currency.valueOf(request.getCurrency()));

                    RequestOptions requestOptions = RequestOptions.builder()
                .withCreatedBy("super-agent")
                .withReason("Create new user")
                .withComment("Created via Super Agent platform")
                .build();

            Account createdAccount = accountApi.createAccount(account, requestOptions);
            log.info("✅ 账户创建成功，账户ID: {}", createdAccount.getAccountId());

            // 2. 如果有初始积分，添加积分
            BigDecimal balance = BigDecimal.ZERO;
            if (request.getInitialCredit() != null && request.getInitialCredit().compareTo(BigDecimal.ZERO) > 0) {
                addCreditToAccount(createdAccount.getAccountId(), request.getInitialCredit());
                balance = request.getInitialCredit();
                log.info("✅ 为用户 {} 添加 {} 积分", request.getName(), request.getInitialCredit());
            }

            // 3. 构造响应
            return buildAccountResponse(createdAccount, balance);

        } catch (KillBillClientException e) {
            log.error("❌ Kill Bill客户端异常: {}", e.getMessage(), e);
            throw new RuntimeException("创建账户失败: " + e.getMessage(), e);
        } catch (Exception e) {
            log.error("❌ 创建Kill Bill账户失败: {}", e.getMessage(), e);
            throw new RuntimeException("创建账户失败: " + e.getMessage(), e);
        }
    }

    /**
     * 给账户添加积分 - 使用InvoiceApi的createExternalCharges
     */
    public void addCreditToAccount(UUID accountId, BigDecimal creditAmount) {
        try {
            log.info("🎯 为账户 {} 添加 {} 积分", accountId, creditAmount);

                    RequestOptions requestOptions = RequestOptions.builder()
                .withCreatedBy("super-agent")
                .withReason("Add credit")
                .withComment("User credit purchase: " + creditAmount)
                .build();

            // 使用官方推荐的InvoiceApi
            InvoiceApi invoiceApi = new InvoiceApi(killBillClient);
            
            // 创建InvoiceItem用于积分（负数表示信用）
            InvoiceItem creditItem = new InvoiceItem();
            creditItem.setAccountId(accountId);
            creditItem.setAmount(creditAmount.negate()); // 负数表示信用
            creditItem.setCurrency(Currency.USD); // 默认使用USD
            creditItem.setDescription("Credit top-up: " + creditAmount);
            
            // 创建InvoiceItems集合
            List<InvoiceItem> items = new ArrayList<>();
            items.add(creditItem);
            InvoiceItems invoiceItems = new InvoiceItems();
            invoiceItems.addAll(items);
            
            // 调用createExternalCharges
            invoiceApi.createExternalCharges(accountId, invoiceItems, null, Collections.emptyMap(), requestOptions);

            log.info("✅ 成功为账户 {} 添加 {} 积分", accountId, creditAmount);

        } catch (KillBillClientException e) {
            log.error("❌ Kill Bill客户端异常: {}", e.getMessage(), e);
            throw new RuntimeException("添加积分失败: " + e.getMessage(), e);
        } catch (Exception e) {
            log.error("❌ 添加积分失败: {}", e.getMessage(), e);
            throw new RuntimeException("添加积分失败: " + e.getMessage(), e);
        }
    }

    /**
     * 查询账户余额 - 使用AccountApi的getAccount方法
     */
    public BigDecimal getAccountBalance(UUID accountId) {
        try {
            log.info("🔍 查询账户 {} 余额", accountId);

            RequestOptions requestOptions = RequestOptions.builder().build();
            AccountApi accountApi = new AccountApi(killBillClient);
            
            // 使用getAccount方法并设置accountWithBalance=true来获取余额信息
            Account account = accountApi.getAccount(accountId, true, false, AuditLevel.NONE, requestOptions);
            
            if (account != null && account.getAccountBalance() != null) {
                return account.getAccountBalance();
            }
            
            return BigDecimal.ZERO;

        } catch (KillBillClientException e) {
            log.warn("⚠️ 查询账户余额异常: {}", e.getMessage());
            return BigDecimal.ZERO;
        } catch (Exception e) {
            log.error("❌ 查询账户余额失败: {}", e.getMessage(), e);
            return BigDecimal.ZERO;
        }
    }

    /**
     * 根据外部键查询账户 - 使用AccountApi
     */
    public KillBillAccountResponse getAccountByExternalKey(String externalKey) {
        try {
            log.info("🔍 查询账户: {}", externalKey);

            RequestOptions requestOptions = RequestOptions.builder().build();
            AccountApi accountApi = new AccountApi(killBillClient);
            
            // 使用getAccountByKey方法，设置accountWithBalance=true来同时获取余额
            Account account = accountApi.getAccountByKey(externalKey, true, false, AuditLevel.NONE, requestOptions);
            if (account == null) {
                throw new RuntimeException("账户不存在: " + externalKey);
            }
            
            BigDecimal balance = account.getAccountBalance() != null ? account.getAccountBalance() : BigDecimal.ZERO;
            return buildAccountResponse(account, balance);

        } catch (KillBillClientException e) {
            log.error("❌ Kill Bill客户端异常: {}", e.getMessage(), e);
            throw new RuntimeException("查询账户失败: " + e.getMessage(), e);
        } catch (Exception e) {
            log.error("❌ 查询账户失败: {}", e.getMessage(), e);
            throw new RuntimeException("查询账户失败: " + e.getMessage(), e);
        }
    }

    /**
     * 消费积分 - 使用InvoiceApi的createExternalCharges
     */
    public boolean consumeCredit(UUID accountId, BigDecimal amount, String description) {
        try {
            // 先检查余额
            BigDecimal currentBalance = getAccountBalance(accountId);
            if (currentBalance.compareTo(amount) < 0) {
                log.warn("⚠️ 余额不足，当前余额: {}, 需要消费: {}", currentBalance, amount);
                return false;
            }

            log.info("💰 消费积分 - 账户: {}, 金额: {}, 描述: {}", accountId, amount, description);

                    RequestOptions requestOptions = RequestOptions.builder()
                .withCreatedBy("super-agent")
                .withReason("Credit consumption")
                .withComment(description)
                .build();

            // 使用官方推荐的InvoiceApi
            InvoiceApi invoiceApi = new InvoiceApi(killBillClient);
            
            // 创建InvoiceItem用于消费（正数表示费用）
            InvoiceItem chargeItem = new InvoiceItem();
            chargeItem.setAccountId(accountId);
            chargeItem.setAmount(amount); // 正数表示费用
            chargeItem.setCurrency(Currency.USD); // 默认使用USD
            chargeItem.setDescription(description);
            
            // 创建InvoiceItems集合
            List<InvoiceItem> items = new ArrayList<>();
            items.add(chargeItem);
            InvoiceItems invoiceItems = new InvoiceItems();
            invoiceItems.addAll(items);
            
            // 调用createExternalCharges
            invoiceApi.createExternalCharges(accountId, invoiceItems, null, Collections.emptyMap(), requestOptions);

            log.info("✅ 成功消费积分");
            return true;

        } catch (KillBillClientException e) {
            log.error("❌ Kill Bill客户端异常: {}", e.getMessage(), e);
            return false;
        } catch (Exception e) {
            log.error("❌ 消费积分失败: {}", e.getMessage(), e);
            return false;
        }
    }

    /**
     * 构建账户响应对象
     */
    private KillBillAccountResponse buildAccountResponse(Account account, BigDecimal balance) {
        KillBillAccountResponse response = new KillBillAccountResponse();
        response.setAccountId(account.getAccountId().toString());
        response.setName(account.getName());
        response.setEmail(account.getEmail());
        response.setExternalKey(account.getExternalKey());
        response.setCurrency(account.getCurrency() != null ? account.getCurrency().toString() : "USD");
        response.setAccountBalance(balance);
        
        // 暂时使用当前时间，因为Account模型可能不包含createdDate字段
        response.setCreatedDate(LocalDateTime.now());
        
        return response;
    }
}