package com.gxy.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.core.util.IdUtil;
import com.gxy.common.exception.BusinessException;
import com.gxy.mapper.CouponMapper;
import com.gxy.mapper.FarmStayMapper;
import com.gxy.model.dto.CouponRequest;
import com.gxy.model.dto.CouponResponse;
import com.gxy.model.entity.Coupon;
import com.gxy.model.entity.FarmStay;
import com.gxy.model.enums.UserType;
import com.gxy.service.CouponService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CouponServiceImpl implements CouponService {

    private final CouponMapper couponMapper;
    private final FarmStayMapper farmStayMapper;

    @Override
    public CouponResponse create(CouponRequest request) {
        enforceOperator();
        Coupon coupon = new Coupon();
        coupon.setCode(IdUtil.simpleUUID());
        coupon.setTitle(request.getTitle());
        coupon.setDescription(request.getDescription());
        coupon.setDiscountAmount(request.getDiscountAmount());
        coupon.setMinimumSpend(request.getMinimumSpend());
        coupon.setValidFrom(request.getValidFrom());
        coupon.setValidTo(request.getValidTo());
        validateOwner(request.getFarmStayId());
        coupon.setFarmStayId(request.getFarmStayId());
        coupon.setTotalCount(request.getTotalCount());
        coupon.setUsedCount(0);
        coupon.setStatus(request.getStatus() == null ? "ACTIVE" : request.getStatus());
        couponMapper.insert(coupon);
        return toResponse(coupon);
    }

    @Override
    public List<CouponResponse> listAvailable(Long farmStayId) {
        Date now = new Date();
        return couponMapper.listAvailable(now, farmStayId)
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    public BigDecimal calculateDiscount(String code, BigDecimal orderAmount, Long farmStayId) {
        if (code == null || code.isEmpty()) {
            return BigDecimal.ZERO;
        }
        Coupon coupon = couponMapper.selectByCode(code);
        if (!isUsable(coupon, farmStayId)) {
            return BigDecimal.ZERO;
        }
        if (orderAmount.compareTo(coupon.getMinimumSpend()) < 0) {
            return BigDecimal.ZERO;
        }
        int changed = couponMapper.increaseUsedCount(coupon.getId());
        if (changed == 0) {
            throw new BusinessException("优惠券已被抢完");
        }
        return coupon.getDiscountAmount();
    }

    private boolean isUsable(Coupon coupon, Long farmStayId) {
        if (coupon == null) {
            return false;
        }
        Date now = new Date();
        boolean timeValid = coupon.getValidFrom().before(now) && coupon.getValidTo().after(now);
        boolean statusValid = Objects.equals("ACTIVE", coupon.getStatus());
        boolean countValid = coupon.getUsedCount() < coupon.getTotalCount();
        boolean scopeValid = coupon.getFarmStayId() == null || Objects.equals(coupon.getFarmStayId(), farmStayId);
        return timeValid && statusValid && countValid && scopeValid;
    }

    private void enforceOperator() {
        if (!Objects.equals(UserType.OPERATOR.getCode(), StpUtil.getLoginType())) {
            throw new BusinessException("仅经营者可创建优惠券");
        }
    }

    private void validateOwner(Long farmStayId) {
        if (farmStayId == null) {
            return;
        }
        Long ownerId = StpUtil.getLoginIdAsLong();
        FarmStay farmStay = farmStayMapper.selectByIdAndOwner(farmStayId, ownerId);
        if (farmStay == null) {
            throw new BusinessException("仅能为自己的农家乐创建优惠券");
        }
    }

    private CouponResponse toResponse(Coupon coupon) {
        CouponResponse response = new CouponResponse();
        response.setId(coupon.getId());
        response.setCode(coupon.getCode());
        response.setTitle(coupon.getTitle());
        response.setDescription(coupon.getDescription());
        response.setDiscountAmount(coupon.getDiscountAmount());
        response.setMinimumSpend(coupon.getMinimumSpend());
        response.setValidFrom(coupon.getValidFrom());
        response.setValidTo(coupon.getValidTo());
        response.setFarmStayId(coupon.getFarmStayId());
        response.setTotalCount(coupon.getTotalCount());
        response.setUsedCount(coupon.getUsedCount());
        response.setStatus(coupon.getStatus());
        return response;
    }
}
