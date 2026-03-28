package com.gxy.service;

import com.gxy.model.entity.RechargeOrder;

import java.util.Map;

public interface AlipayRechargeService {

    String createRechargeQrCode(RechargeOrder rechargeOrder);

    boolean verifyNotify(Map<String, String> params);

    RechargeTradeStatus queryRecharge(String rechargeNo);

    record RechargeTradeStatus(boolean success, String tradeStatus, String tradeNo, String detail) {
    }
}
