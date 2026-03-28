package com.gxy.service.impl;

import com.alipay.api.AlipayApiException;
import com.alipay.api.AlipayClient;
import com.alipay.api.DefaultAlipayClient;
import com.alipay.api.internal.util.AlipaySignature;
import com.alipay.api.request.AlipayTradePrecreateRequest;
import com.alipay.api.request.AlipayTradeQueryRequest;
import com.alipay.api.response.AlipayTradePrecreateResponse;
import com.alipay.api.response.AlipayTradeQueryResponse;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gxy.common.exception.BusinessException;
import com.gxy.config.AlipayProperties;
import com.gxy.model.entity.RechargeOrder;
import com.gxy.service.AlipayRechargeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.LinkedHashMap;
import java.util.Map;

@Service
@Slf4j
@RequiredArgsConstructor
public class AlipayRechargeServiceImpl implements AlipayRechargeService {

    private final AlipayProperties alipayProperties;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public String createRechargeQrCode(RechargeOrder rechargeOrder) {
        ensureEnabledAndConfigured();
        try {
            AlipayTradePrecreateRequest request = new AlipayTradePrecreateRequest();
            request.setNotifyUrl(alipayProperties.getNotifyUrl());
            request.setBizContent(buildPrecreateBizContent(rechargeOrder));
            AlipayTradePrecreateResponse response = createClient().execute(request);
            if (response == null || !response.isSuccess() || !StringUtils.hasText(response.getQrCode())) {
                String message = response == null
                        ? "支付宝无响应"
                        : String.format("code=%s, msg=%s, subCode=%s, subMsg=%s, body=%s",
                        response.getCode(),
                        response.getMsg(),
                        response.getSubCode(),
                        response.getSubMsg(),
                        response.getBody());
                log.warn("Alipay precreate failed. rechargeNo={}, detail={}", rechargeOrder.getRechargeNo(), message);
                throw new BusinessException("支付宝预下单失败: " + message);
            }
            return response.getQrCode();
        } catch (AlipayApiException e) {
            log.warn("Alipay precreate exception. rechargeNo={}, errCode={}, errMsg={}",
                    rechargeOrder.getRechargeNo(), e.getErrCode(), e.getErrMsg());
            String detail = String.format("errCode=%s, errMsg=%s, message=%s",
                    e.getErrCode(), e.getErrMsg(), e.getMessage());
            throw new BusinessException("支付宝预下单失败: " + detail);
        }
    }

    @Override
    public boolean verifyNotify(Map<String, String> params) {
        ensureEnabledAndConfigured();
        try {
            return AlipaySignature.rsaCheckV1(
                    params,
                    alipayProperties.getAlipayPublicKey(),
                    alipayProperties.getCharset(),
                    alipayProperties.getSignType()
            );
        } catch (AlipayApiException e) {
            throw new BusinessException("支付宝回调验签失败: " + e.getErrMsg());
        }
    }

    @Override
    public RechargeTradeStatus queryRecharge(String rechargeNo) {
        ensureEnabledAndConfigured();
        try {
            AlipayTradeQueryRequest request = new AlipayTradeQueryRequest();
            Map<String, Object> bizContent = new LinkedHashMap<>();
            bizContent.put("out_trade_no", rechargeNo);
            request.setBizContent(objectMapper.writeValueAsString(bizContent));
            AlipayTradeQueryResponse response = createClient().execute(request);
            if (response == null) {
                return new RechargeTradeStatus(false, null, null, "response is null");
            }
            if (!response.isSuccess()) {
                String detail = String.format("code=%s, msg=%s, subCode=%s, subMsg=%s",
                        response.getCode(), response.getMsg(), response.getSubCode(), response.getSubMsg());
                return new RechargeTradeStatus(false, response.getTradeStatus(), response.getTradeNo(), detail);
            }
            return new RechargeTradeStatus(true, response.getTradeStatus(), response.getTradeNo(), response.getBody());
        } catch (AlipayApiException | JsonProcessingException e) {
            String detail = e instanceof AlipayApiException apiException
                    ? String.format("errCode=%s, errMsg=%s, message=%s",
                    apiException.getErrCode(), apiException.getErrMsg(), apiException.getMessage())
                    : e.getMessage();
            log.warn("Alipay trade query failed. rechargeNo={}, detail={}", rechargeNo, detail);
            return new RechargeTradeStatus(false, null, null, detail);
        }
    }

    private AlipayClient createClient() {
        return new DefaultAlipayClient(
                alipayProperties.getGatewayUrl(),
                alipayProperties.getAppId(),
                alipayProperties.getPrivateKey(),
                alipayProperties.getFormat(),
                alipayProperties.getCharset(),
                alipayProperties.getAlipayPublicKey(),
                alipayProperties.getSignType()
        );
    }

    private void ensureEnabledAndConfigured() {
        if (!alipayProperties.isEnabled()) {
            throw new BusinessException("支付宝充值未启用，请先配置支付参数");
        }
        if (!StringUtils.hasText(alipayProperties.getAppId())
                || !StringUtils.hasText(alipayProperties.getPrivateKey())
                || !StringUtils.hasText(alipayProperties.getAlipayPublicKey())
                || !StringUtils.hasText(alipayProperties.getNotifyUrl())) {
            throw new BusinessException("支付宝支付配置不完整，请补充 appId、私钥、公钥和回调地址");
        }
    }

    private String buildPrecreateBizContent(RechargeOrder rechargeOrder) {
        Map<String, Object> bizContent = new LinkedHashMap<>();
        bizContent.put("out_trade_no", rechargeOrder.getRechargeNo());
        bizContent.put("total_amount", rechargeOrder.getAmount().toPlainString());
        bizContent.put("subject", rechargeOrder.getSubject());
        try {
            return objectMapper.writeValueAsString(bizContent);
        } catch (JsonProcessingException e) {
            throw new BusinessException("构建支付宝下单参数失败");
        }
    }
}
