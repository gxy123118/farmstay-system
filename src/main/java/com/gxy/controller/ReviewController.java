package com.gxy.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import com.gxy.common.ApiResponse;
import com.gxy.model.dto.ReviewRequest;
import com.gxy.model.dto.ReviewResponse;
import com.gxy.service.ReviewService;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;

/**
 * 评论与评分接口
 */
@RestController
@RequestMapping("/api/reviews")
@Validated
@RequiredArgsConstructor
public class ReviewController {

    private final ReviewService reviewService;

    /**
     * 游客提交评价
     */
    @PostMapping
    public ApiResponse<ReviewResponse> create(@Valid @RequestBody ReviewRequest request) {
        return ApiResponse.ok(reviewService.createReview(request));
    }

    /**
     * 查询农家乐评价
     */
    @GetMapping
    public ApiResponse<List<ReviewResponse>> list(@RequestParam Long farmStayId) {
        return ApiResponse.ok(reviewService.listForFarmStay(farmStayId));
    }

    /**
     * 经营者审核评价
     */
    @PutMapping("/{id}/status")
    public ApiResponse<ReviewResponse> moderate(@PathVariable Long id, @RequestParam String status) {
        return ApiResponse.ok(reviewService.moderate(id, status));
    }
}
