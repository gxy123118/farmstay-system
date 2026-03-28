package com.gxy.controller;

import com.gxy.common.ApiResponse;
import com.gxy.model.dto.BalanceFlowResponse;
import com.gxy.model.dto.BalanceResponse;
import com.gxy.model.dto.RechargeCreateRequest;
import com.gxy.model.dto.RechargeResponse;
import com.gxy.service.AccountService;
import jakarta.validation.Valid;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@Validated
@RequiredArgsConstructor
@RequestMapping("/api/account")
public class AccountController {

    private final AccountService accountService;

    @GetMapping("/balance")
    public ApiResponse<BalanceResponse> getBalance() {
        return ApiResponse.ok(accountService.getBalance());
    }

    @GetMapping("/balance/flows")
    public ApiResponse<List<BalanceFlowResponse>> listBalanceFlows() {
        return ApiResponse.ok(accountService.listBalanceFlows());
    }

    @PostMapping("/recharges")
    public ApiResponse<RechargeResponse> createRecharge(@Valid @RequestBody RechargeCreateRequest request) {
        return ApiResponse.ok(accountService.createRecharge(request));
    }

    @GetMapping("/recharges/{rechargeNo}")
    public ApiResponse<RechargeResponse> getRecharge(@PathVariable String rechargeNo) {
        return ApiResponse.ok(accountService.getRecharge(rechargeNo));
    }

    @PostMapping("/recharges/{rechargeNo}/mock-pay")
    public ApiResponse<RechargeResponse> mockPay(@PathVariable String rechargeNo) {
        return ApiResponse.ok(accountService.mockPayRecharge(rechargeNo));
    }

    @PostMapping("/recharges/alipay/notify")
    public String alipayNotify(HttpServletRequest request) {
        Map<String, String> params = request.getParameterMap()
                .entrySet()
                .stream()
                .collect(Collectors.toMap(Map.Entry::getKey, entry -> entry.getValue()[0]));
        return accountService.handleAlipayNotify(params);
    }
}
