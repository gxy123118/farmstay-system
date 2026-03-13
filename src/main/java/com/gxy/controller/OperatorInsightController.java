package com.gxy.controller;

import com.gxy.common.ApiResponse;
import com.gxy.model.dto.OperatorInsightIssueResponse;
import com.gxy.model.dto.OperatorInsightReportResponse;
import com.gxy.service.OperatorInsightService;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/operator/insights/reviews")
@Validated
@RequiredArgsConstructor
public class OperatorInsightController {

    private final OperatorInsightService operatorInsightService;

    @PostMapping("/{farmStayId}/generate")
    public ApiResponse<OperatorInsightReportResponse> generate(@PathVariable Long farmStayId,
                                                               @RequestParam(required = false, defaultValue = "30") Integer periodDays) {
        return ApiResponse.ok(operatorInsightService.generate(farmStayId, periodDays));
    }

    @GetMapping("/{farmStayId}")
    public ApiResponse<OperatorInsightReportResponse> latest(@PathVariable Long farmStayId) {
        return ApiResponse.ok(operatorInsightService.latest(farmStayId));
    }

    @GetMapping("/{farmStayId}/history")
    public ApiResponse<List<OperatorInsightReportResponse>> history(@PathVariable Long farmStayId) {
        return ApiResponse.ok(operatorInsightService.history(farmStayId));
    }

    @GetMapping("/{farmStayId}/history/{reportId}")
    public ApiResponse<OperatorInsightReportResponse> historyDetail(@PathVariable Long farmStayId,
                                                                    @PathVariable Long reportId) {
        return ApiResponse.ok(operatorInsightService.historyDetail(farmStayId, reportId));
    }

    @DeleteMapping("/{farmStayId}/history/{reportId}")
    public ApiResponse<Boolean> deleteHistory(@PathVariable Long farmStayId,
                                              @PathVariable Long reportId) {
        return ApiResponse.ok(operatorInsightService.deleteHistory(farmStayId, reportId));
    }

    @GetMapping("/{farmStayId}/issues")
    public ApiResponse<List<OperatorInsightIssueResponse>> issues(@PathVariable Long farmStayId) {
        return ApiResponse.ok(operatorInsightService.issues(farmStayId));
    }
}
