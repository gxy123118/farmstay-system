package com.gxy.service;

import com.gxy.common.PageResponse;
import com.gxy.model.dto.AdminDashboardResponse;
import com.gxy.model.dto.AdminReviewResponse;
import com.gxy.model.dto.AdminUserResponse;

public interface AdminConsoleService {

    PageResponse<AdminUserResponse> listUsers(String keyword, String userType, String status, Integer page, Integer pageSize);

    AdminUserResponse updateUserStatus(Long userId, String status);

    PageResponse<AdminReviewResponse> listReviews(String keyword, Integer page, Integer pageSize);

    void deleteReview(Long reviewId);

    AdminDashboardResponse dashboard();
}
