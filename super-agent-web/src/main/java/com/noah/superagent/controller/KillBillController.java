package com.noah.superagent.controller;

import com.noah.superagent.common.dto.killbill.CreateAccountRequest;
import com.noah.superagent.common.dto.killbill.KillBillAccountResponse;
import com.noah.superagent.killbill.service.KillBillService;
import com.noah.superagent.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.math.BigDecimal;
import java.util.UUID;

/**
 * Kill Bill 计费系统控制器
 *
 * @author Noah
 * @since 1.0.0
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/killbill")
@RequiredArgsConstructor
@Validated
@Tag(name = "Kill Bill计费系统", description = "Kill Bill计费系统相关接口")
public class KillBillController {

    private final KillBillService killBillService;

    @PostMapping("/accounts")
    @Operation(summary = "创建账户并添加积分", description = "在Kill Bill中创建新账户并添加初始积分")
    public ApiResponse<KillBillAccountResponse> createAccount(@Valid @RequestBody CreateAccountRequest request) {
        log.info("接收创建Kill Bill账户请求: {}", request.getName());
        
        KillBillAccountResponse response = killBillService.createAccountWithCredit(request);
        return ApiResponse.success("账户创建成功", response);
    }

    @GetMapping("/accounts/{externalKey}")
    @Operation(summary = "查询账户信息", description = "根据外部键查询账户信息和余额")
    public ApiResponse<KillBillAccountResponse> getAccount(
            @Parameter(description = "外部键", example = "user_zhangsan_001") 
            @PathVariable("externalKey") String externalKey) {
        log.info("接收查询Kill Bill账户请求: {}", externalKey);
        
        KillBillAccountResponse response = killBillService.getAccountByExternalKey(externalKey);
        return ApiResponse.success("查询成功", response);
    }

    @PostMapping("/accounts/{accountId}/credits")
    @Operation(summary = "添加积分", description = "给指定账户添加积分")
    public ApiResponse<Void> addCredit(
            @Parameter(description = "账户ID") 
            @PathVariable("accountId") String accountId,
            @Parameter(description = "积分金额", example = "1000.00") 
            @RequestParam("amount") BigDecimal amount) {
        log.info("接收添加积分请求 - 账户: {}, 金额: {}", accountId, amount);
        
        UUID uuid = UUID.fromString(accountId);
        killBillService.addCreditToAccount(uuid, amount);
        return ApiResponse.success("积分添加成功");
    }

    @GetMapping("/accounts/{accountId}/balance")
    @Operation(summary = "查询账户余额", description = "查询指定账户的当前余额")
    public ApiResponse<BigDecimal> getBalance(
            @Parameter(description = "账户ID") 
            @PathVariable("accountId") String accountId) {
        log.info("接收查询余额请求 - 账户: {}", accountId);
        
        UUID uuid = UUID.fromString(accountId);
        BigDecimal balance = killBillService.getAccountBalance(uuid);
        return ApiResponse.success("查询成功", balance);
    }

    @PostMapping("/accounts/{accountId}/consume")
    @Operation(summary = "消费积分", description = "消费指定账户的积分")
    public ApiResponse<Boolean> consumeCredit(
            @Parameter(description = "账户ID") 
            @PathVariable("accountId") String accountId,
            @Parameter(description = "消费金额", example = "10.00") 
            @RequestParam("amount") BigDecimal amount,
            @Parameter(description = "消费描述", example = "使用GPT-4模型消费") 
            @RequestParam("description") String description) {
        log.info("接收消费积分请求 - 账户: {}, 金额: {}, 描述: {}", accountId, amount, description);
        
        UUID uuid = UUID.fromString(accountId);
        boolean success = killBillService.consumeCredit(uuid, amount, description);
        return ApiResponse.success("操作完成", success);
    }

    @PostMapping("/test/create-user-with-1000-credits")
    @Operation(summary = "测试接口：创建用户并添加1000积分", description = "快速测试接口，创建张三用户并添加1000积分")
    public ApiResponse<KillBillAccountResponse> testCreateUserWith1000Credits() {
        log.info("🚀 开始测试：创建用户并添加1000积分");
        
        CreateAccountRequest request = new CreateAccountRequest();
        request.setName("测试用户张三");
        request.setEmail("zhangsan@example.com");
        request.setExternalKey("test_user_zhangsan_" + System.currentTimeMillis());
        request.setCurrency("USD");
        request.setInitialCredit(new BigDecimal("1000.00"));
        
        KillBillAccountResponse response = killBillService.createAccountWithCredit(request);
        log.info("✅ 测试完成 - 用户ID: {}, 余额: {}", response.getAccountId(), response.getAccountBalance());
        
        return ApiResponse.success("测试成功！用户创建完成，已添加1000积分", response);
    }
}
