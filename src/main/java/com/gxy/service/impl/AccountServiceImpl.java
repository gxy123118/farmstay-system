package com.gxy.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.core.util.IdUtil;
import com.gxy.common.auth.AuthGuard;
import com.gxy.common.exception.BusinessException;
import com.gxy.mapper.RechargeOrderMapper;
import com.gxy.mapper.RefundOrderMapper;
import com.gxy.mapper.UserAccountMapper;
import com.gxy.mapper.UserBalanceFlowMapper;
import com.gxy.model.dto.BalanceFlowResponse;
import com.gxy.model.dto.BalanceResponse;
import com.gxy.model.dto.RechargeCreateRequest;
import com.gxy.model.dto.RechargeResponse;
import com.gxy.model.entity.RechargeOrder;
import com.gxy.model.entity.RefundOrder;
import com.gxy.model.entity.UserAccount;
import com.gxy.model.entity.UserBalanceFlow;
import com.gxy.service.AccountService;
import com.gxy.service.AlipayRechargeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class AccountServiceImpl implements AccountService {

    private static final String PAY_METHOD_ALIPAY = "ALIPAY";
    private static final String CHANNEL_BALANCE = "BALANCE";
    private static final String RECHARGE_SUCCESS = "SUCCESS";
    private static final String RECHARGE_PENDING = "PENDING";

    private final UserAccountMapper userAccountMapper;
    private final UserBalanceFlowMapper userBalanceFlowMapper;
    private final RechargeOrderMapper rechargeOrderMapper;
    private final RefundOrderMapper refundOrderMapper;
    private final AlipayRechargeService alipayRechargeService;

    @Override
    public BalanceResponse getBalance() {
        AuthGuard.enforceVisitor();
        UserAccount account = getRequiredAccount(StpUtil.getLoginIdAsLong());
        BalanceResponse response = new BalanceResponse();
        response.setBalance(defaultBalance(account.getBalance()));
        return response;
    }

    @Override
    public List<BalanceFlowResponse> listBalanceFlows() {
        AuthGuard.enforceVisitor();
        return userBalanceFlowMapper.selectByUserId(StpUtil.getLoginIdAsLong())
                .stream()
                .map(this::toFlowResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public RechargeResponse createRecharge(RechargeCreateRequest request) {
        AuthGuard.enforceVisitor();
        if (request.getAmount() == null || request.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException("充值金额必须大于 0");
        }

        RechargeOrder order = new RechargeOrder();
        order.setRechargeNo(IdUtil.getSnowflakeNextIdStr());
        order.setUserId(StpUtil.getLoginIdAsLong());
        order.setAmount(request.getAmount());
        order.setPayMethod(PAY_METHOD_ALIPAY);
        order.setStatus(RECHARGE_PENDING);
        order.setSubject("平台余额充值");
        rechargeOrderMapper.insert(order);

        String payInfo;
        String qrCode = null;
        try {
            qrCode = alipayRechargeService.createRechargeQrCode(order);
            payInfo = "请使用支付宝扫码完成充值";
        } catch (BusinessException ex) {
            log.warn("Alipay recharge precreate failed. rechargeNo={}, message={}", order.getRechargeNo(), ex.getMessage());
            payInfo = ex.getMessage();
        }

        RechargeOrder latest = rechargeOrderMapper.selectByRechargeNoAndUser(order.getRechargeNo(), order.getUserId());
        return toRechargeResponse(latest == null ? order : latest, payInfo, qrCode);
    }

    @Override
    @Transactional
    public RechargeResponse getRecharge(String rechargeNo) {
        AuthGuard.enforceVisitor();
        Long userId = StpUtil.getLoginIdAsLong();
        RechargeOrder order = rechargeOrderMapper.selectByRechargeNoAndUser(rechargeNo, userId);
        if (order == null) {
            throw new BusinessException("充值单不存在");
        }

        if (RECHARGE_PENDING.equals(order.getStatus())) {
            AlipayRechargeService.RechargeTradeStatus tradeStatus = alipayRechargeService.queryRecharge(rechargeNo);
            log.info("Alipay recharge query result. rechargeNo={}, success={}, tradeStatus={}, detail={}",
                    rechargeNo, tradeStatus.success(), tradeStatus.tradeStatus(), tradeStatus.detail());
            if (tradeStatus.success()
                    && ("TRADE_SUCCESS".equals(tradeStatus.tradeStatus()) || "TRADE_FINISHED".equals(tradeStatus.tradeStatus()))) {
                finalizeRechargeSuccess(order, tradeStatus.tradeNo(), tradeStatus.detail(), "支付宝充值");
                order = rechargeOrderMapper.selectByRechargeNoAndUser(rechargeNo, userId);
            } else if ("TRADE_CLOSED".equals(tradeStatus.tradeStatus())) {
                rechargeOrderMapper.markFailed(rechargeNo, tradeStatus.detail());
                order = rechargeOrderMapper.selectByRechargeNoAndUser(rechargeNo, userId);
            }
        }

        return toRechargeResponse(order, null, null);
    }

    @Override
    @Transactional
    public RechargeResponse mockPayRecharge(String rechargeNo) {
        AuthGuard.enforceVisitor();
        Long userId = StpUtil.getLoginIdAsLong();
        RechargeOrder order = rechargeOrderMapper.selectByRechargeNoAndUser(rechargeNo, userId);
        if (order == null) {
            throw new BusinessException("充值单不存在");
        }
        if (RECHARGE_SUCCESS.equals(order.getStatus())) {
            return toRechargeResponse(order, null, null);
        }
        if (!RECHARGE_PENDING.equals(order.getStatus())) {
            throw new BusinessException("当前充值单状态不可支付");
        }

        finalizeRechargeSuccess(order, "MOCK-" + rechargeNo, "mock recharge success", "余额充值");
        RechargeOrder latest = rechargeOrderMapper.selectByRechargeNoAndUser(rechargeNo, userId);
        return toRechargeResponse(latest, null, null);
    }

    @Override
    @Transactional
    public String handleAlipayNotify(Map<String, String> params) {
        if (params == null || params.isEmpty()) {
            return "failure";
        }
        if (!alipayRechargeService.verifyNotify(params)) {
            return "failure";
        }

        String rechargeNo = params.get("out_trade_no");
        String tradeNo = params.get("trade_no");
        String tradeStatus = params.get("trade_status");
        RechargeOrder order = rechargeOrderMapper.selectByRechargeNo(rechargeNo);
        if (order == null) {
            return "failure";
        }
        if (RECHARGE_SUCCESS.equals(order.getStatus())) {
            return "success";
        }
        if (!"TRADE_SUCCESS".equals(tradeStatus) && !"TRADE_FINISHED".equals(tradeStatus)) {
            rechargeOrderMapper.markFailed(rechargeNo, params.toString());
            return "failure";
        }

        finalizeRechargeSuccess(order, tradeNo, params.toString(), "支付宝充值");
        return "success";
    }

    @Override
    @Transactional
    public BigDecimal payOrder(Long userId, String orderNo, BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) < 0) {
            throw new BusinessException("支付金额不合法");
        }
        UserAccount account = getRequiredAccount(userId);
        BigDecimal balanceBefore = defaultBalance(account.getBalance());
        if (amount.compareTo(BigDecimal.ZERO) == 0) {
            return balanceBefore;
        }
        int changed = userAccountMapper.decreaseBalance(userId, amount);
        if (changed == 0) {
            throw new BusinessException(409, "余额不足，请先充值");
        }
        BigDecimal balanceAfter = balanceBefore.subtract(amount);
        insertBalanceFlow(userId, "PAY_ORDER", orderNo, amount.negate(), balanceBefore, balanceAfter, "订单余额支付");
        return balanceAfter;
    }

    @Override
    @Transactional
    public void refundOrder(Long userId, Long orderId, String orderNo, BigDecimal amount, String reason) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) < 0) {
            throw new BusinessException("退款金额不合法");
        }
        RefundOrder existing = refundOrderMapper.selectByOrderId(orderId);
        if (existing != null && RECHARGE_SUCCESS.equals(existing.getStatus())) {
            throw new BusinessException("订单已退款");
        }

        UserAccount account = getRequiredAccount(userId);
        BigDecimal balanceBefore = defaultBalance(account.getBalance());
        userAccountMapper.increaseBalance(userId, amount);
        BigDecimal balanceAfter = balanceBefore.add(amount);

        RefundOrder refundOrder = new RefundOrder();
        refundOrder.setRefundNo(IdUtil.getSnowflakeNextIdStr());
        refundOrder.setOrderId(orderId);
        refundOrder.setOrderNo(orderNo);
        refundOrder.setUserId(userId);
        refundOrder.setRefundAmount(amount);
        refundOrder.setRefundChannel(CHANNEL_BALANCE);
        refundOrder.setStatus(RECHARGE_SUCCESS);
        refundOrder.setReason(reason);
        refundOrder.setRefundedAt(new Date());
        refundOrderMapper.insert(refundOrder);
        insertBalanceFlow(userId, "REFUND", refundOrder.getRefundNo(), amount, balanceBefore, balanceAfter, reason);
    }

    private UserAccount getRequiredAccount(Long userId) {
        UserAccount account = userAccountMapper.selectById(userId);
        if (account == null) {
            throw new BusinessException("账户不存在");
        }
        return account;
    }

    private void finalizeRechargeSuccess(RechargeOrder order, String tradeNo, String notifyContent, String remark) {
        UserAccount account = getRequiredAccount(order.getUserId());
        BigDecimal balanceBefore = defaultBalance(account.getBalance());
        int changed = rechargeOrderMapper.markSuccess(order.getRechargeNo(), tradeNo, notifyContent);
        if (changed == 0) {
            return;
        }
        userAccountMapper.increaseBalance(order.getUserId(), order.getAmount());
        BigDecimal balanceAfter = balanceBefore.add(order.getAmount());
        insertBalanceFlow(order.getUserId(), "RECHARGE", order.getRechargeNo(), order.getAmount(), balanceBefore, balanceAfter, remark);
    }

    private void insertBalanceFlow(Long userId, String changeType, String bizNo, BigDecimal amount,
                                   BigDecimal balanceBefore, BigDecimal balanceAfter, String remark) {
        UserBalanceFlow flow = new UserBalanceFlow();
        flow.setFlowNo(IdUtil.getSnowflakeNextIdStr());
        flow.setUserId(userId);
        flow.setChangeType(changeType);
        flow.setBizNo(bizNo);
        flow.setAmount(amount);
        flow.setBalanceBefore(balanceBefore);
        flow.setBalanceAfter(balanceAfter);
        flow.setRemark(remark);
        userBalanceFlowMapper.insert(flow);
    }

    private BalanceFlowResponse toFlowResponse(UserBalanceFlow flow) {
        BalanceFlowResponse response = new BalanceFlowResponse();
        BeanUtils.copyProperties(flow, response);
        return response;
    }

    private RechargeResponse toRechargeResponse(RechargeOrder order, String payInfo, String qrCode) {
        RechargeResponse response = new RechargeResponse();
        response.setRechargeNo(order.getRechargeNo());
        response.setAmount(order.getAmount());
        response.setPayMethod(order.getPayMethod());
        response.setStatus(order.getStatus());
        response.setPayInfo(payInfo);
        response.setQrCode(qrCode);
        response.setCreatedAt(order.getCreatedAt());
        response.setPaidAt(order.getPaidAt());
        return response;
    }

    private BigDecimal defaultBalance(BigDecimal balance) {
        return balance == null ? BigDecimal.ZERO : balance;
    }
}
