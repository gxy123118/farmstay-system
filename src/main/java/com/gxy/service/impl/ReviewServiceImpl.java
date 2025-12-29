package com.gxy.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import com.gxy.common.exception.BusinessException;
import com.gxy.mapper.BookingOrderMapper;
import com.gxy.mapper.FarmStayMapper;
import com.gxy.mapper.ReviewMapper;
import com.gxy.model.dto.ReviewRequest;
import com.gxy.model.dto.ReviewResponse;
import com.gxy.model.entity.BookingOrder;
import com.gxy.model.entity.FarmStay;
import com.gxy.model.entity.Review;
import com.gxy.model.enums.UserType;
import com.gxy.service.ReviewService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ReviewServiceImpl implements ReviewService {

    private final ReviewMapper reviewMapper;
    private final BookingOrderMapper bookingOrderMapper;
    private final FarmStayMapper farmStayMapper;

    @Override
    public ReviewResponse createReview(ReviewRequest request) {
        enforceVisitor();
        BookingOrder order = bookingOrderMapper.selectById(request.getOrderId());
        if (order == null || !Objects.equals(order.getVisitorId(), StpUtil.getLoginIdAsLong())) {
            throw new BusinessException("订单不存在或无权评价");
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
        review.setStatus("PENDING");
        review.setCreatedAt(new java.util.Date());
        reviewMapper.insert(review);
        return toResponse(review);
    }

    @Override
    public List<ReviewResponse> listForFarmStay(Long farmStayId) {
        return reviewMapper.listApprovedByFarmStay(farmStayId)
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    public ReviewResponse moderate(Long reviewId, String status) {
        enforceOperator();
        Review review = reviewMapper.selectById(reviewId);
        if (review == null) {
            throw new BusinessException("评价不存在");
        }
        ensureOwner(review.getFarmStayId());
        reviewMapper.updateStatus(reviewId, status);
        Review latest = reviewMapper.selectById(reviewId);
        return toResponse(latest);
    }

    private void enforceVisitor() {
        if (!Objects.equals(UserType.VISITOR.getCode(), StpUtil.getLoginType())) {
            throw new BusinessException("仅游客可提交评价");
        }
    }

    private void enforceOperator() {
        if (!Objects.equals(UserType.OPERATOR.getCode(), StpUtil.getLoginType())) {
            throw new BusinessException("仅经营者可审核评价");
        }
    }

    private void ensureOwner(Long farmStayId) {
        Long ownerId = StpUtil.getLoginIdAsLong();
        FarmStay farmStay = farmStayMapper.selectByIdAndOwner(farmStayId, ownerId);
        if (farmStay == null) {
            throw new BusinessException("仅能审核自己农家乐的评价");
        }
    }

    private ReviewResponse toResponse(Review review) {
        ReviewResponse response = new ReviewResponse();
        response.setId(review.getId());
        response.setOrderId(review.getOrderId());
        response.setFarmStayId(review.getFarmStayId());
        response.setVisitorId(review.getVisitorId());
        response.setRating(review.getRating());
        response.setContent(review.getContent());
        response.setStatus(review.getStatus());
        response.setCreatedAt(review.getCreatedAt());
        return response;
    }
}
