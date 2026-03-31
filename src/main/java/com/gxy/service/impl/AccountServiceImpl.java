package com.gxy.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.core.util.IdUtil;
import com.gxy.common.PageResponse;
import com.gxy.common.auth.AuthGuard;
import com.gxy.common.exception.BusinessException;
import com.gxy.mapper.RechargeOrderMapper;
import com.gxy.mapper.RefundOrderMapper;
import com.gxy.mapper.UserAccountMapper;
import com.gxy.mapper.UserBalanceFlowMapper;
import com.gxy.mapper.UserMapper;
import com.gxy.mapper.WithdrawOrderMapper;
import com.gxy.model.dto.AdminWithdrawResponse;
import com.gxy.model.dto.BalanceFlowResponse;
import com.gxy.model.dto.BalanceResponse;
import com.gxy.model.dto.RechargeCreateRequest;
import com.gxy.model.dto.RechargeResponse;
import com.gxy.model.dto.WithdrawCreateRequest;
import com.gxy.model.dto.WithdrawResponse;
import com.gxy.model.entity.RechargeOrder;
import com.gxy.model.entity.RefundOrder;
import com.gxy.model.entity.User;
import com.gxy.model.entity.UserAccount;
import com.gxy.model.entity.UserBalanceFlow;
import com.gxy.model.entity.WithdrawOrder;
import com.gxy.service.AccountService;
import com.gxy.service.AlipayRechargeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Collections;
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
    private static final String CN_MAINLAND_MOBILE_REGEX = "^1\\d{10}$";

    private static final String RECHARGE_PENDING = "PENDING";
    private static final String RECHARGE_SUCCESS = "SUCCESS";
    private static final String RECHARGE_FAILED = "FAILED";

    private static final String WITHDRAW_PENDING = "PENDING";
    private static final String WITHDRAW_APPROVED = "APPROVED";
    private static final String WITHDRAW_SUCCESS = "SUCCESS";
    private static final String WITHDRAW_REJECTED = "REJECTED";

    private final UserAccountMapper userAccountMapper;
    private final UserBalanceFlowMapper userBalanceFlowMapper;
    private final RechargeOrderMapper rechargeOrderMapper;
    private final RefundOrderMapper refundOrderMapper;
    private final AlipayRechargeService alipayRechargeService;
    private final WithdrawOrderMapper withdrawOrderMapper;
    private final UserMapper userMapper;

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
            throw new BusinessException("当前充值单状态不允许模拟支付");
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

    @Override
    @Transactional
    public BigDecimal settleOrderToOperator(Long operatorId, Long orderId, String orderNo, BigDecimal amount, String remark) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) < 0) {
            throw new BusinessException("结算金额不合法");
        }
        UserAccount account = getRequiredAccount(operatorId);
        BigDecimal balanceBefore = defaultBalance(account.getBalance());
        userAccountMapper.increaseBalance(operatorId, amount);
        BigDecimal balanceAfter = balanceBefore.add(amount);
        insertBalanceFlow(
                operatorId,
                "ORDER_SETTLEMENT",
                orderNo,
                amount,
                balanceBefore,
                balanceAfter,
                remark == null || remark.isBlank() ? "订单核销完成，结算到经营者余额" : remark
        );
        return balanceAfter;
    }

    @Override
    @Transactional
    public WithdrawResponse createWithdraw(WithdrawCreateRequest request) {
        AuthGuard.enforceOperator();
        Long operatorId = StpUtil.getLoginIdAsLong();
        if (!PAY_METHOD_ALIPAY.equalsIgnoreCase(request.getChannel())) {
            throw new BusinessException("当前仅支持支付宝人工打款提现");
        }
        if (request.getAccountNo() == null || !request.getAccountNo().matches(CN_MAINLAND_MOBILE_REGEX)) {
            throw new BusinessException("支付宝收款账号必须为合法的 11 位手机号");
        }

        BigDecimal amount = request.getAmount();
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException("提现金额必须大于 0");
        }

        UserAccount account = getRequiredAccount(operatorId);
        BigDecimal balanceBefore = defaultBalance(account.getBalance());
        int changed = userAccountMapper.decreaseBalance(operatorId, amount);
        if (changed == 0) {
            throw new BusinessException("可用余额不足，无法发起提现");
        }
        BigDecimal balanceAfter = balanceBefore.subtract(amount);

        WithdrawOrder order = new WithdrawOrder();
        order.setWithdrawNo(IdUtil.getSnowflakeNextIdStr());
        order.setUserId(operatorId);
        order.setAmount(amount);
        order.setChannel(PAY_METHOD_ALIPAY);
        order.setAccountName(request.getAccountName());
        order.setAccountNo(request.getAccountNo());
        order.setStatus(WITHDRAW_PENDING);
        order.setRemark(request.getRemark());
        withdrawOrderMapper.insert(order);

        insertBalanceFlow(operatorId, "WITHDRAW_APPLY", order.getWithdrawNo(), amount.negate(), balanceBefore, balanceAfter, "经营者提现申请");
        return toWithdrawResponse(order);
    }

    @Override
    public List<WithdrawResponse> listWithdraws() {
        AuthGuard.enforceOperator();
        return withdrawOrderMapper.selectByUserId(StpUtil.getLoginIdAsLong())
                .stream()
                .map(this::toWithdrawResponse)
                .collect(Collectors.toList());
    }

    @Override
    public PageResponse<AdminWithdrawResponse> listAdminWithdraws(String status, Integer page, Integer pageSize) {
        AuthGuard.enforceAdmin();
        int currentPage = page == null || page < 1 ? 1 : page;
        int currentPageSize = pageSize == null || pageSize < 1 ? 10 : Math.min(pageSize, 100);
        int offset = (currentPage - 1) * currentPageSize;

        List<WithdrawOrder> orders = withdrawOrderMapper.selectAdminPage(status, offset, currentPageSize);
        long total = withdrawOrderMapper.countAdminPage(status);

        List<Long> userIds = orders.stream().map(WithdrawOrder::getUserId).distinct().collect(Collectors.toList());
        Map<Long, User> userMap = userIds.isEmpty()
                ? Collections.emptyMap()
                : userMapper.selectByIds(userIds).stream()
                .collect(Collectors.toMap(User::getId, user -> user, (first, second) -> first));

        List<AdminWithdrawResponse> responses = orders.stream()
                .map(order -> toAdminWithdrawResponse(order, userMap.get(order.getUserId())))
                .collect(Collectors.toList());
        return PageResponse.of(responses, total, currentPage, currentPageSize);
    }

    @Override
    @Transactional
    public AdminWithdrawResponse approveWithdraw(Long withdrawId, String reviewRemark) {
        AuthGuard.enforceAdmin();
        getRequiredWithdraw(withdrawId);
        int changed = withdrawOrderMapper.updateReviewStatus(withdrawId, WITHDRAW_PENDING, WITHDRAW_APPROVED, reviewRemark);
        if (changed == 0) {
            throw new BusinessException("提现单当前状态不允许审核通过");
        }
        return buildAdminWithdrawResponse(withdrawId);
    }

    @Override
    @Transactional
    public AdminWithdrawResponse rejectWithdraw(Long withdrawId, String reviewRemark) {
        AuthGuard.enforceAdmin();
        WithdrawOrder order = getRequiredWithdraw(withdrawId);
        int changed = withdrawOrderMapper.updateReviewStatus(withdrawId, WITHDRAW_PENDING, WITHDRAW_REJECTED, reviewRemark);
        if (changed == 0) {
            throw new BusinessException("提现单当前状态不允许审核拒绝");
        }

        UserAccount account = getRequiredAccount(order.getUserId());
        BigDecimal balanceBefore = defaultBalance(account.getBalance());
        userAccountMapper.increaseBalance(order.getUserId(), order.getAmount());
        BigDecimal balanceAfter = balanceBefore.add(order.getAmount());
        insertBalanceFlow(order.getUserId(), "WITHDRAW_REJECT_RETURN", order.getWithdrawNo(), order.getAmount(), balanceBefore, balanceAfter, "提现申请被驳回，金额退回余额");
        return buildAdminWithdrawResponse(withdrawId);
    }

    @Override
    @Transactional
    public AdminWithdrawResponse completeWithdrawTransfer(Long withdrawId, String transferNo, String reviewRemark) {
        AuthGuard.enforceAdmin();
        WithdrawOrder order = getRequiredWithdraw(withdrawId);
        if (!WITHDRAW_APPROVED.equals(order.getStatus())) {
            throw new BusinessException("仅审核通过的提现单可确认打款完成");
        }
        int changed = withdrawOrderMapper.markSuccess(withdrawId, transferNo, reviewRemark);
        if (changed == 0) {
            throw new BusinessException("提现单打款确认失败");
        }
        return buildAdminWithdrawResponse(withdrawId);
    }

    private UserAccount getRequiredAccount(Long userId) {
        UserAccount account = userAccountMapper.selectById(userId);
        if (account == null) {
            throw new BusinessException("账户不存在");
        }
        return account;
    }

    private WithdrawOrder getRequiredWithdraw(Long withdrawId) {
        WithdrawOrder order = withdrawOrderMapper.selectById(withdrawId);
        if (order == null) {
            throw new BusinessException("提现单不存在");
        }
        return order;
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

    private WithdrawResponse toWithdrawResponse(WithdrawOrder order) {
        WithdrawResponse response = new WithdrawResponse();
        BeanUtils.copyProperties(order, response);
        return response;
    }

    private AdminWithdrawResponse buildAdminWithdrawResponse(Long withdrawId) {
        WithdrawOrder latest = getRequiredWithdraw(withdrawId);
        User user = userMapper.selectById(latest.getUserId());
        return toAdminWithdrawResponse(latest, user);
    }

    private AdminWithdrawResponse toAdminWithdrawResponse(WithdrawOrder order, User user) {
        AdminWithdrawResponse response = new AdminWithdrawResponse();
        BeanUtils.copyProperties(order, response);
        if (user != null) {
            response.setUsername(user.getUsername());
            response.setDisplayName(user.getDisplayName());
        }
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
