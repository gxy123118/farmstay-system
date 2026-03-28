package com.gxy.controller;

import com.gxy.common.ApiResponse;
import com.gxy.common.PageResponse;
import com.gxy.model.dto.AdminReviewResponse;
import com.gxy.service.AdminConsoleService;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Validated
@RequiredArgsConstructor
@RequestMapping("/api/admin/reviews")
public class AdminReviewController {

    private final AdminConsoleService adminConsoleService;

    @GetMapping
    public ApiResponse<PageResponse<AdminReviewResponse>> list(@RequestParam(required = false) String keyword,
                                                               @RequestParam(defaultValue = "1") Integer page,
                                                               @RequestParam(defaultValue = "10") Integer pageSize) {
        return ApiResponse.ok(adminConsoleService.listReviews(keyword, page, pageSize));
    }

    @DeleteMapping("/{reviewId}")
    public ApiResponse<Void> delete(@PathVariable Long reviewId) {
        adminConsoleService.deleteReview(reviewId);
        return ApiResponse.ok();
    }
}
