package com.gxy.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.core.util.IdUtil;
import com.gxy.common.auth.AuthGuard;
import com.gxy.common.exception.BusinessException;
import com.gxy.mapper.BookingOrderMapper;
import com.gxy.mapper.FarmStayMapper;
import com.gxy.mapper.ReviewMapper;
import com.gxy.mapper.RoomTypeMapper;
import com.gxy.model.dto.BookingRequest;
import com.gxy.model.dto.BookingResponse;
import com.gxy.model.dto.OrderStatusUpdateRequest;
import com.gxy.model.dto.PaymentRequest;
import com.gxy.model.dto.PaymentResponse;
import com.gxy.model.entity.BookingOrder;
import com.gxy.model.entity.FarmStay;
import com.gxy.model.entity.RoomType;
import com.gxy.model.entity.Review;
import com.gxy.model.dto.FarmStayResponse;
import com.gxy.model.dto.RoomResponse;
import com.gxy.model.vo.BookingDetailVo;
import com.gxy.service.BookingService;
import com.gxy.service.CouponService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class BookingServiceImpl implements BookingService {

    private final BookingOrderMapper bookingOrderMapper;
    private final RoomTypeMapper roomTypeMapper;
    private final FarmStayMapper farmStayMapper;
    private final ReviewMapper reviewMapper;
    private final CouponService couponService;

    private static final String STATUS_CREATED = "CREATED";
    private static final String STATUS_PAID = "PAID";
    private static final String STATUS_CANCELLED = "CANCELLED";
    private static final String STATUS_REFUNDED = "REFUNDED";

    @Override
    public BookingResponse createOrder(BookingRequest request) {
        AuthGuard.enforceVisitor();
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
        AuthGuard.enforceVisitor();
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
        AuthGuard.enforceVisitor();
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
    public BookingResponse refund(Long orderId) {
        AuthGuard.enforceVisitor();
        Long visitorId = StpUtil.getLoginIdAsLong();
        BookingOrder order = bookingOrderMapper.selectById(orderId);
        if (order == null || !Objects.equals(order.getVisitorId(), visitorId)) {
            throw new BusinessException("订单不存在或无权退款");
        }
        if (!Objects.equals(STATUS_PAID, order.getStatus())) {
            throw new BusinessException("当前订单状态无法退款");
        }
        int changed = bookingOrderMapper.updateStatusByVisitor(orderId, visitorId, STATUS_REFUNDED);
        if (changed == 0) {
            throw new BusinessException("退款失败");
        }
        BookingOrder latest = bookingOrderMapper.selectById(orderId);
        return toResponse(latest);
    }

    @Override
    public BookingResponse updateStatus(OrderStatusUpdateRequest request) {
        AuthGuard.enforceOperator();
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
    public List<BookingDetailVo> listMyOrders() {
        AuthGuard.enforceVisitor();
        Long visitorId = StpUtil.getLoginIdAsLong();
        return buildDetailList(bookingOrderMapper.selectByVisitor(visitorId));
    }

    @Override
    public List<BookingDetailVo> listOwnerOrders(Long farmStayId) {
        AuthGuard.enforceOperator();
        ensureOwner(farmStayId);
        return buildDetailList(bookingOrderMapper.selectByFarmStay(farmStayId));
    }

    private long calculateNights(Date checkIn, Date checkOut) {
        return ChronoUnit.DAYS.between(
                checkIn.toInstant().atZone(ZoneId.systemDefault()).toLocalDate(),
                checkOut.toInstant().atZone(ZoneId.systemDefault()).toLocalDate());
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

    private List<BookingDetailVo> buildDetailList(List<BookingOrder> orders) {
        Map<Long, FarmStay> farmStayCache = new HashMap<>();
        Map<Long, RoomType> roomTypeCache = new HashMap<>();
        Map<Long, Review> reviewMap = new HashMap<>();
        if (!orders.isEmpty()) {
            List<Long> orderIds = orders.stream().map(BookingOrder::getId).collect(Collectors.toList());
            reviewMap = reviewMapper.selectByOrderIds(orderIds)
                    .stream()
                    .collect(Collectors.toMap(Review::getOrderId, review -> review, (first, second) -> first));
        }
        Map<Long, Review> finalReviewMap = reviewMap;
        return orders.stream()
                .map(order -> toDetailVo(order, farmStayCache, roomTypeCache, finalReviewMap))
                .collect(Collectors.toList());
    }

    private BookingDetailVo toDetailVo(
            BookingOrder order,
            Map<Long, FarmStay> farmStayCache,
            Map<Long, RoomType> roomTypeCache,
            Map<Long, Review> reviewMap
    ) {
        BookingResponse base = toResponse(order);
        BookingDetailVo detail = new BookingDetailVo();
        BeanUtils.copyProperties(base, detail);
        FarmStay farmStay = farmStayCache.computeIfAbsent(order.getFarmStayId(), farmStayMapper::selectById);
        detail.setFarmStay(farmStay == null ? null : toFarmStayResponse(farmStay));
        RoomType roomType = roomTypeCache.computeIfAbsent(order.getRoomTypeId(), roomTypeMapper::selectById);
        detail.setRoom(roomType == null ? null : toRoomResponse(roomType));
        detail.setReviewed(reviewMap.containsKey(order.getId()));
        return detail;
    }

    private FarmStayResponse toFarmStayResponse(FarmStay farmStay) {
        if (farmStay == null) {
            return null;
        }
        FarmStayResponse response = new FarmStayResponse();
        response.setId(farmStay.getId());
        response.setOwnerId(farmStay.getOwnerId());
        response.setName(farmStay.getName());
        response.setCity(farmStay.getCity());
        response.setAddress(farmStay.getAddress());
        response.setDescription(farmStay.getDescription());
        response.setPriceRange(farmStay.getPriceRange());
        response.setPriceLevel(farmStay.getPriceLevel());
        response.setAverageRating(farmStay.getAverageRating());
        response.setCoverImage(farmStay.getCoverImage());
        response.setContactPhone(farmStay.getContactPhone());
        response.setTags(farmStay.getTags());
        response.setStatus(farmStay.getStatus());
        response.setCreatedAt(farmStay.getCreatedAt());
        response.setUpdatedAt(farmStay.getUpdatedAt());
        return response;
    }

    private RoomResponse toRoomResponse(RoomType roomType) {
        if (roomType == null) {
            return null;
        }
        RoomResponse response = new RoomResponse();
        response.setId(roomType.getId());
        response.setFarmStayId(roomType.getFarmStayId());
        response.setName(roomType.getName());
        response.setDescription(roomType.getDescription());
        response.setBedType(roomType.getBedType());
        response.setMaxGuests(roomType.getMaxGuests());
        response.setPrice(roomType.getPrice());
        response.setStock(roomType.getStock());
        response.setTags(roomType.getTags());
        response.setStatus(roomType.getStatus());
        response.setCreatedAt(roomType.getCreatedAt());
        response.setUpdatedAt(roomType.getUpdatedAt());
        return response;
    }
}
