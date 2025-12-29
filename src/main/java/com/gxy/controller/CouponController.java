package com.gxy.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import com.gxy.common.ApiResponse;
import com.gxy.model.dto.CouponRequest;
import com.gxy.model.dto.CouponResponse;
import com.gxy.service.CouponService;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;

/**
 * 优惠券与活动接口
 */
@RestController
@RequestMapping("/api/coupons")
@Validated
@RequiredArgsConstructor
public class CouponController {

    private final CouponService couponService;

    /**
     * 经营者创建或上架优惠券
     */
    @SaCheckLogin
    @PostMapping
    public ApiResponse<CouponResponse> create(@Valid @RequestBody CouponRequest request) {
        return ApiResponse.ok(couponService.create(request));
    }

    /**
     * 查询当前可用的优惠券（可按农家乐过滤）
     */
    @GetMapping
    public ApiResponse<List<CouponResponse>> listAvailable(@RequestParam(required = false) Long farmStayId) {
        return ApiResponse.ok(couponService.listAvailable(farmStayId));
    }
}
