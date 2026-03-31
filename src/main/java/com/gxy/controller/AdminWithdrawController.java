package com.gxy.controller;

import com.gxy.common.ApiResponse;
import com.gxy.common.PageResponse;
import com.gxy.model.dto.AdminWithdrawCompleteRequest;
import com.gxy.model.dto.AdminWithdrawResponse;
import com.gxy.model.dto.AdminWithdrawReviewRequest;
import com.gxy.service.AccountService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/withdraws")
@RequiredArgsConstructor
public class AdminWithdrawController {

    private final AccountService accountService;

    @GetMapping
    public ApiResponse<PageResponse<AdminWithdrawResponse>> list(
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer pageSize) {
        return ApiResponse.ok(accountService.listAdminWithdraws(status, page, pageSize));
    }

    @PostMapping("/{withdrawId}/approve")
    public ApiResponse<AdminWithdrawResponse> approve(
            @PathVariable Long withdrawId,
            @Valid @RequestBody AdminWithdrawReviewRequest request) {
        return ApiResponse.ok(accountService.approveWithdraw(withdrawId, request.getReviewRemark()));
    }

    @PostMapping("/{withdrawId}/reject")
    public ApiResponse<AdminWithdrawResponse> reject(
            @PathVariable Long withdrawId,
            @Valid @RequestBody AdminWithdrawReviewRequest request) {
        return ApiResponse.ok(accountService.rejectWithdraw(withdrawId, request.getReviewRemark()));
    }

    @PostMapping("/{withdrawId}/complete-transfer")
    public ApiResponse<AdminWithdrawResponse> completeTransfer(
            @PathVariable Long withdrawId,
            @Valid @RequestBody AdminWithdrawCompleteRequest request) {
        return ApiResponse.ok(accountService.completeWithdrawTransfer(
                withdrawId,
                request.getTransferNo(),
                request.getReviewRemark()));
    }
}
