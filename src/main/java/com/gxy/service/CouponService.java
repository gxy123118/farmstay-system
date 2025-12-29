package com.gxy.service;

import com.gxy.model.dto.CouponRequest;
import com.gxy.model.dto.CouponResponse;

import java.math.BigDecimal;
import java.util.List;

public interface CouponService {

    CouponResponse create(CouponRequest request);

    List<CouponResponse> listAvailable(Long farmStayId);

    BigDecimal calculateDiscount(String code, BigDecimal orderAmount, Long farmStayId);
}
