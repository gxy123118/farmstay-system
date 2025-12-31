package com.gxy.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.core.util.IdUtil;
import com.gxy.common.exception.BusinessException;
import com.gxy.mapper.BookingOrderMapper;
import com.gxy.mapper.FarmStayMapper;
import com.gxy.mapper.RoomTypeMapper;
import com.gxy.model.dto.BookingRequest;
import com.gxy.model.dto.BookingResponse;
import com.gxy.model.dto.OrderStatusUpdateRequest;
import com.gxy.model.dto.PaymentRequest;
import com.gxy.model.dto.PaymentResponse;
import com.gxy.model.entity.BookingOrder;
import com.gxy.model.entity.FarmStay;
import com.gxy.model.entity.RoomType;
import com.gxy.model.enums.UserType;
import com.gxy.service.BookingService;
import com.gxy.service.CouponService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class BookingServiceImpl implements BookingService {

    private final BookingOrderMapper bookingOrderMapper;
    private final RoomTypeMapper roomTypeMapper;
    private final FarmStayMapper farmStayMapper;
    private final CouponService couponService;

    private static final String STATUS_CREATED = "CREATED";
    private static final String STATUS_PAID = "PAID";
    private static final String STATUS_CANCELLED = "CANCELLED";

    @Override
    public BookingResponse createOrder(BookingRequest request) {
        enforceVisitor();
        FarmStay farmStay = farmStayMapper.selectById(request.getFarmStayId());
        if (farmStay == null) {
            throw new BusinessException("农家乐不存在");
        }
        RoomType roomType = roomTypeMapper.selectById(request.getRoomTypeId());
        if (roomType == null || !Objects.equals(roomType.getFarmStayId(), request.getFarmStayId())) {
            throw new BusinessException("房型不存在或不属于该农家乐");
        }
        long nights = calculateNights(request.getCheckInDate(), request.getCheckOutDate());
        if (nights <= 0) {
            throw new BusinessException("离店日期必须晚于入住日期");
        }
        BookingOrder order = new BookingOrder();
        order.setOrderNo(IdUtil.getSnowflakeNextIdStr());
        order.setVisitorId(StpUtil.getLoginIdAsLong());
        order.setFarmStayId(request.getFarmStayId());
        order.setRoomTypeId(request.getRoomTypeId());
        order.setCheckInDate(request.getCheckInDate());
        order.setCheckOutDate(request.getCheckOutDate());
        order.setGuests(request.getGuests());
        BigDecimal amount = roomType.getPrice().multiply(BigDecimal.valueOf(nights));
        BigDecimal discount = couponService.calculateDiscount(request.getCouponCode(), amount, request.getFarmStayId());
        BigDecimal payable = amount.subtract(discount);
        if (payable.compareTo(BigDecimal.ZERO) < 0) {
            payable = BigDecimal.ZERO;
        }
        order.setTotalAmount(payable);
        order.setStatus(STATUS_CREATED);
        order.setPaymentChannel("UNPAID");
        order.setContactName(request.getContactName());
        order.setContactPhone(request.getContactPhone());
        order.setCouponCode(request.getCouponCode());
        order.setRemarks(request.getRemarks());
        Date now = new Date();
        order.setCreatedAt(now);
        order.setUpdatedAt(now);
        bookingOrderMapper.insert(order);
        return toResponse(order);
    }

    @Override
    public BookingResponse cancel(Long orderId) {
        enforceVisitor();
        Long visitorId = StpUtil.getLoginIdAsLong();
        BookingOrder order = bookingOrderMapper.selectById(orderId);
        if (order == null || !Objects.equals(visitorId, order.getVisitorId())) {
            throw new BusinessException("订单不存在或无权取消");
        }
        if (Objects.equals(STATUS_PAID, order.getStatus())) {
            throw new BusinessException("已支付订单请联系客服处理退款");
        }
        int changed = bookingOrderMapper.updateStatusByVisitor(orderId, visitorId, STATUS_CANCELLED);
        if (changed == 0) {
            throw new BusinessException("订单取消失败或无权取消");
        }
        BookingOrder latest = bookingOrderMapper.selectById(orderId);
        return toResponse(latest);
    }

    @Override
    public PaymentResponse pay(PaymentRequest request) {
        enforceVisitor();
        BookingOrder order = bookingOrderMapper.selectById(request.getOrderId());
        if (order == null || !Objects.equals(order.getVisitorId(), StpUtil.getLoginIdAsLong())) {
            throw new BusinessException("订单不存在或无权支付");
        }
        if (!Objects.equals(STATUS_CREATED, order.getStatus())) {
            throw new BusinessException("订单状态不可支付");
        }
        bookingOrderMapper.updateStatus(request.getOrderId(), STATUS_PAID, request.getChannel());
        return new PaymentResponse("模拟支付成功，支付渠道：" + request.getChannel(), STATUS_PAID);
    }

    @Override
    public BookingResponse updateStatus(OrderStatusUpdateRequest request) {
        enforceOperator();
        BookingOrder order = bookingOrderMapper.selectById(request.getOrderId());
        if (order == null) {
            throw new BusinessException("订单不存在");
        }
        ensureOwner(order.getFarmStayId());
        bookingOrderMapper.updateStatus(request.getOrderId(), request.getStatus(), request.getPaymentChannel());
        BookingOrder latest = bookingOrderMapper.selectById(request.getOrderId());
        return toResponse(latest);
    }

    @Override
    public List<BookingResponse> listMyOrders() {
        enforceVisitor();
        Long visitorId = StpUtil.getLoginIdAsLong();
        return bookingOrderMapper.selectByVisitor(visitorId).stream().map(this::toResponse).collect(Collectors.toList());
    }

    @Override
    public List<BookingResponse> listOwnerOrders(Long farmStayId) {
        enforceOperator();
        ensureOwner(farmStayId);
        return bookingOrderMapper.selectByFarmStay(farmStayId).stream().map(this::toResponse).collect(Collectors.toList());
    }

    private long calculateNights(Date checkIn, Date checkOut) {
        return ChronoUnit.DAYS.between(
                checkIn.toInstant().atZone(ZoneId.systemDefault()).toLocalDate(),
                checkOut.toInstant().atZone(ZoneId.systemDefault()).toLocalDate());
    }

    private void enforceVisitor() {
        if (!Objects.equals(UserType.VISITOR.getCode(), StpUtil.getSession().get("userType"))) {
            throw new BusinessException("仅游客可执行该操作");
        }
    }

    private void enforceOperator() {
        if (!Objects.equals(UserType.OPERATOR.getCode(), StpUtil.getSession().get("userType"))) {
            throw new BusinessException("仅经营者可执行该操作");
        }
    }

    private void ensureOwner(Long farmStayId) {
        Long ownerId = StpUtil.getLoginIdAsLong();
        FarmStay farmStay = farmStayMapper.selectByIdAndOwner(farmStayId, ownerId);
        if (farmStay == null) {
            throw new BusinessException("只能管理自己名下的订单");
        }
    }

    private BookingResponse toResponse(BookingOrder order) {
        BookingResponse response = new BookingResponse();
        response.setId(order.getId());
        response.setOrderNo(order.getOrderNo());
        response.setVisitorId(order.getVisitorId());
        response.setFarmStayId(order.getFarmStayId());
        response.setRoomTypeId(order.getRoomTypeId());
        response.setCheckInDate(order.getCheckInDate());
        response.setCheckOutDate(order.getCheckOutDate());
        response.setGuests(order.getGuests());
        response.setTotalAmount(order.getTotalAmount());
        response.setStatus(order.getStatus());
        response.setPaymentChannel(order.getPaymentChannel());
        response.setContactName(order.getContactName());
        response.setContactPhone(order.getContactPhone());
        response.setCouponCode(order.getCouponCode());
        response.setRemarks(order.getRemarks());
        response.setCreatedAt(order.getCreatedAt());
        response.setUpdatedAt(order.getUpdatedAt());
        return response;
    }
}
