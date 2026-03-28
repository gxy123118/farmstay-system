package com.gxy.service;

import com.gxy.model.dto.BalanceFlowResponse;
import com.gxy.model.dto.BalanceResponse;
import com.gxy.model.dto.RechargeCreateRequest;
import com.gxy.model.dto.RechargeResponse;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public interface AccountService {

    BalanceResponse getBalance();

    List<BalanceFlowResponse> listBalanceFlows();

    RechargeResponse createRecharge(RechargeCreateRequest request);

    RechargeResponse getRecharge(String rechargeNo);

    RechargeResponse mockPayRecharge(String rechargeNo);

    String handleAlipayNotify(Map<String, String> params);

    BigDecimal payOrder(Long userId, String orderNo, BigDecimal amount);

    void refundOrder(Long userId, Long orderId, String orderNo, BigDecimal amount, String reason);
}
