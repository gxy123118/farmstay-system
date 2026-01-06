package com.gxy.service;

import com.gxy.model.dto.BookingRequest;
import com.gxy.model.dto.BookingResponse;
import com.gxy.model.dto.OrderStatusUpdateRequest;
import com.gxy.model.dto.PaymentRequest;
import com.gxy.model.dto.PaymentResponse;
import com.gxy.model.vo.BookingDetailVo;

import java.util.List;

public interface BookingService {

    BookingResponse createOrder(BookingRequest request);

    BookingResponse cancel(Long orderId);

    PaymentResponse pay(PaymentRequest request);

    BookingResponse updateStatus(OrderStatusUpdateRequest request);

    BookingResponse refund(Long orderId);

    List<BookingDetailVo> listMyOrders();

    List<BookingDetailVo> listOwnerOrders(Long farmStayId);
}
