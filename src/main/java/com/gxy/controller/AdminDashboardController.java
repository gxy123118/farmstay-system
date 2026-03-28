package com.gxy.controller;

import com.gxy.common.ApiResponse;
import com.gxy.model.dto.AdminDashboardResponse;
import com.gxy.service.AdminConsoleService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/dashboard")
public class AdminDashboardController {

    private final AdminConsoleService adminConsoleService;

    @GetMapping("/overview")
    public ApiResponse<AdminDashboardResponse> overview() {
        return ApiResponse.ok(adminConsoleService.dashboard());
    }
}
