package com.gxy.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import com.gxy.common.ApiResponse;
import com.gxy.model.dto.BookingRequest;
import com.gxy.model.dto.BookingResponse;
import com.gxy.model.dto.OrderStatusUpdateRequest;
import com.gxy.model.dto.PaymentRequest;
import com.gxy.model.dto.PaymentResponse;
import com.gxy.service.BookingService;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;
import java.util.List;

/**
 * 预订与订单管理接口
 */
@RestController
@RequestMapping("/api/bookings")
@Validated
@RequiredArgsConstructor
public class BookingController {

    private final BookingService bookingService;

    @SaCheckLogin
    @PostMapping
    public ApiResponse<BookingResponse> create(@Valid @RequestBody BookingRequest request) {
        return ApiResponse.ok(bookingService.createOrder(request));
    }

    @SaCheckLogin
    @PostMapping("/{orderId}/cancel")
    public ApiResponse<BookingResponse> cancel(@PathVariable Long orderId) {
        return ApiResponse.ok(bookingService.cancel(orderId));
    }

    /**
     * 支付接口框架（模拟支付成功）
     */
    @SaCheckLogin
    @PostMapping("/pay")
    public ApiResponse<PaymentResponse> pay(@Valid @RequestBody PaymentRequest request) {
        return ApiResponse.ok(bookingService.pay(request));
    }

    /**
     * 经营者更新订单状态（确认/完结/退款等）
     */
    @SaCheckLogin
    @PutMapping("/status")
    public ApiResponse<BookingResponse> updateStatus(@Valid @RequestBody OrderStatusUpdateRequest request) {
        return ApiResponse.ok(bookingService.updateStatus(request));
    }

    /**
     * 游客查看自己的订单
     */
    @SaCheckLogin
    @GetMapping("/mine")
    public ApiResponse<List<BookingResponse>> myOrders() {
        return ApiResponse.ok(bookingService.listMyOrders());
    }

    /**
     * 经营者查看名下订单
     */
    @SaCheckLogin
    @GetMapping
    public ApiResponse<List<BookingResponse>> ownerOrders(@RequestParam Long farmStayId) {
        return ApiResponse.ok(bookingService.listOwnerOrders(farmStayId));
    }
}
