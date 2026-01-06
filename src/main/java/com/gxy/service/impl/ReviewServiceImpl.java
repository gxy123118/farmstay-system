package com.gxy.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import com.gxy.common.auth.AuthGuard;
import com.gxy.common.exception.BusinessException;
import com.gxy.mapper.BookingOrderMapper;
import com.gxy.mapper.ReviewMapper;
import com.gxy.mapper.UserMapper;
import com.gxy.model.dto.ReviewRequest;
import com.gxy.model.dto.ReviewResponse;
import com.gxy.model.entity.BookingOrder;
import com.gxy.model.entity.Review;
import com.gxy.model.entity.User;
import com.gxy.service.ReviewService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ReviewServiceImpl implements ReviewService {

    private final ReviewMapper reviewMapper;
    private final BookingOrderMapper bookingOrderMapper;
    private final UserMapper userMapper;

    @Override
    public ReviewResponse createReview(ReviewRequest request) {
        AuthGuard.enforceVisitor();
        BookingOrder order = bookingOrderMapper.selectById(request.getOrderId());
        if (order == null || !Objects.equals(order.getVisitorId(), StpUtil.getLoginIdAsLong())) {
            throw new BusinessException("订单不存在或无权评价");
        }
        Review existing = reviewMapper.selectByOrderId(request.getOrderId());
        if (existing != null) {
            throw new BusinessException("该订单已评价");
        }
        if (!Objects.equals(order.getFarmStayId(), request.getFarmStayId())) {
            throw new BusinessException("订单与农家乐信息不匹配");
        }
        Review review = new Review();
        review.setOrderId(request.getOrderId());
        review.setFarmStayId(request.getFarmStayId());
        review.setVisitorId(order.getVisitorId());
        review.setRating(request.getRating());
        review.setContent(request.getContent());
        review.setCreatedAt(new java.util.Date());
        reviewMapper.insert(review);
        return toResponse(review, null);
    }

    @Override
    public List<ReviewResponse> listForFarmStay(Long farmStayId) {
        List<Review> reviews = reviewMapper.listByFarmStay(farmStayId);
        if (reviews.isEmpty()) {
            return java.util.Collections.emptyList();
        }
        Map<Long, User> userMap = userMapper.selectByIds(
                reviews.stream().map(Review::getVisitorId).distinct().collect(Collectors.toList())
        ).stream().collect(Collectors.toMap(User::getId, user -> user));
        return reviews.stream()
                .map(review -> toResponse(review, userMap.get(review.getVisitorId())))
                .collect(Collectors.toList());
    }

    @Override
    public ReviewResponse getByOrder(Long orderId) {
        AuthGuard.enforceVisitor();
        BookingOrder order = bookingOrderMapper.selectById(orderId);
        if (order == null || !Objects.equals(order.getVisitorId(), StpUtil.getLoginIdAsLong())) {
            throw new BusinessException("订单不存在或无权查看评价");
        }
        Review review = reviewMapper.selectByOrderId(orderId);
        if (review == null) {
            return null;
        }
        User user = userMapper.selectByIds(java.util.Collections.singletonList(review.getVisitorId()))
                .stream()
                .findFirst()
                .orElse(null);
        return toResponse(review, user);
    }

    @Override
    public ReviewResponse updateReview(Long orderId, ReviewRequest request) {
        AuthGuard.enforceVisitor();
        Long visitorId = StpUtil.getLoginIdAsLong();
        BookingOrder order = bookingOrderMapper.selectById(orderId);
        if (order == null || !Objects.equals(order.getVisitorId(), visitorId)) {
            throw new BusinessException("订单不存在或无权修改评价");
        }
        Review review = reviewMapper.selectByOrderId(orderId);
        if (review == null) {
            throw new BusinessException("评价不存在");
        }
        if (!Objects.equals(review.getVisitorId(), visitorId)) {
            throw new BusinessException("无权修改他人评价");
        }
        if (!Objects.equals(order.getFarmStayId(), request.getFarmStayId())) {
            throw new BusinessException("订单与农家乐信息不匹配");
        }
        int changed = reviewMapper.updateByOrder(orderId, visitorId, request.getRating(), request.getContent());
        if (changed == 0) {
            throw new BusinessException("评价更新失败");
        }
        Review latest = reviewMapper.selectByOrderId(orderId);
        User user = userMapper.selectByIds(java.util.Collections.singletonList(visitorId))
                .stream()
                .findFirst()
                .orElse(null);
        return toResponse(latest, user);
    }

    private ReviewResponse toResponse(Review review, User user) {
        ReviewResponse response = new ReviewResponse();
        response.setId(review.getId());
        response.setOrderId(review.getOrderId());
        response.setFarmStayId(review.getFarmStayId());
        response.setVisitorId(review.getVisitorId());
        response.setRating(review.getRating());
        response.setContent(review.getContent());
        response.setCreatedAt(review.getCreatedAt());
        if (user != null) {
            response.setVisitorName(user.getDisplayName() == null || user.getDisplayName().isEmpty()
                    ? user.getUsername()
                    : user.getDisplayName());
        }
        return response;
    }
}
