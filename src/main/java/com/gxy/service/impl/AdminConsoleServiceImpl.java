package com.gxy.service.impl;

import com.gxy.common.PageResponse;
import com.gxy.common.auth.AuthGuard;
import com.gxy.common.exception.BusinessException;
import com.gxy.mapper.BookingOrderMapper;
import com.gxy.mapper.FarmStayMapper;
import com.gxy.mapper.ReviewMapper;
import com.gxy.mapper.UserMapper;
import com.gxy.model.dto.AdminDashboardResponse;
import com.gxy.model.dto.AdminReviewResponse;
import com.gxy.model.dto.AdminUserResponse;
import com.gxy.model.entity.Review;
import com.gxy.model.entity.User;
import com.gxy.model.enums.UserType;
import com.gxy.service.AdminConsoleService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AdminConsoleServiceImpl implements AdminConsoleService {

    private static final String STATUS_ACTIVE = "ACTIVE";
    private static final String STATUS_DISABLED = "DISABLED";

    private final UserMapper userMapper;
    private final ReviewMapper reviewMapper;
    private final BookingOrderMapper bookingOrderMapper;
    private final FarmStayMapper farmStayMapper;

    @Override
    public PageResponse<AdminUserResponse> listUsers(String keyword, String userType, String status, Integer page, Integer pageSize) {
        AuthGuard.enforceAdmin();
        String normalizedUserType = normalizeUserType(userType);
        String normalizedStatus = normalizeStatus(status);
        int currentPage = page == null || page < 1 ? 1 : page;
        int currentPageSize = pageSize == null || pageSize < 1 ? 10 : Math.min(pageSize, 100);
        int offset = (currentPage - 1) * currentPageSize;

        List<User> users = userMapper.selectPageForAdmin(trim(keyword), normalizedUserType, normalizedStatus, offset, currentPageSize);
        long total = userMapper.countPageForAdmin(trim(keyword), normalizedUserType, normalizedStatus);
        List<AdminUserResponse> responses = new ArrayList<>();
        for (User user : users) {
            AdminUserResponse response = new AdminUserResponse();
            BeanUtils.copyProperties(user, response);
            responses.add(response);
        }
        return PageResponse.of(responses, total, currentPage, currentPageSize);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AdminUserResponse updateUserStatus(Long userId, String status) {
        AuthGuard.enforceAdmin();
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }
        if (UserType.ADMIN.getCode().equalsIgnoreCase(user.getUserType())) {
            throw new BusinessException("不支持通过后台接口修改管理员状态");
        }
        String normalizedStatus = normalizeStatus(status);
        if (!STATUS_ACTIVE.equals(normalizedStatus) && !STATUS_DISABLED.equals(normalizedStatus)) {
            throw new BusinessException("status仅支持ACTIVE或DISABLED");
        }
        if (userMapper.updateStatus(userId, normalizedStatus) <= 0) {
            throw new BusinessException("用户状态更新失败");
        }
        User latest = userMapper.selectById(userId);
        AdminUserResponse response = new AdminUserResponse();
        BeanUtils.copyProperties(latest, response);
        return response;
    }

    @Override
    public PageResponse<AdminReviewResponse> listReviews(String keyword, Integer page, Integer pageSize) {
        AuthGuard.enforceAdmin();
        int currentPage = page == null || page < 1 ? 1 : page;
        int currentPageSize = pageSize == null || pageSize < 1 ? 10 : Math.min(pageSize, 100);
        int offset = (currentPage - 1) * currentPageSize;
        List<AdminReviewResponse> reviews = reviewMapper.selectAdminPage(trim(keyword), offset, currentPageSize);
        long total = reviewMapper.countAdminPage(trim(keyword));
        return PageResponse.of(reviews, total, currentPage, currentPageSize);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteReview(Long reviewId) {
        AuthGuard.enforceAdmin();
        Review review = reviewMapper.selectById(reviewId);
        if (review == null) {
            throw new BusinessException("评论不存在");
        }
        if (reviewMapper.deleteById(reviewId) <= 0) {
            throw new BusinessException("评论删除失败");
        }
    }

    @Override
    public AdminDashboardResponse dashboard() {
        AuthGuard.enforceAdmin();
        long orderCount = bookingOrderMapper.countAll();
        BigDecimal turnover = bookingOrderMapper.sumTurnover();
        long paidLikeCount = bookingOrderMapper.countPaidLikeOrders();
        long refundedCount = bookingOrderMapper.countRefundedOrders();

        AdminDashboardResponse response = new AdminDashboardResponse();
        response.setOrderCount(orderCount);
        response.setTurnover(turnover == null ? BigDecimal.ZERO : turnover);
        response.setRefundRate(paidLikeCount == 0 ? 0.0 : round2((double) refundedCount / paidLikeCount));
        response.setFarmStayCount(farmStayMapper.countAll());
        response.setActiveOperatorCount(userMapper.countByUserType(UserType.OPERATOR.getCode()));
        return response;
    }

    private String normalizeUserType(String userType) {
        if (!StringUtils.hasText(userType)) {
            return null;
        }
        UserType resolved = UserType.fromCode(userType.trim());
        if (resolved == null) {
            throw new BusinessException("userType仅支持visitor、operator或admin");
        }
        return resolved.getCode();
    }

    private String normalizeStatus(String status) {
        return StringUtils.hasText(status) ? status.trim().toUpperCase() : null;
    }

    private String trim(String text) {
        return StringUtils.hasText(text) ? text.trim() : null;
    }

    private double round2(double value) {
        return Math.round(value * 100D) / 100D;
    }
}
