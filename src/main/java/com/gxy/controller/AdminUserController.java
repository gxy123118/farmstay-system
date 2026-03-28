package com.gxy.controller;

import com.gxy.common.ApiResponse;
import com.gxy.common.PageResponse;
import com.gxy.model.dto.AdminUserResponse;
import com.gxy.model.dto.AdminUserStatusRequest;
import com.gxy.service.AdminConsoleService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Validated
@RequiredArgsConstructor
@RequestMapping("/api/admin/users")
public class AdminUserController {

    private final AdminConsoleService adminConsoleService;

    @GetMapping
    public ApiResponse<PageResponse<AdminUserResponse>> list(@RequestParam(required = false) String keyword,
                                                             @RequestParam(required = false) String userType,
                                                             @RequestParam(required = false) String status,
                                                             @RequestParam(defaultValue = "1") Integer page,
                                                             @RequestParam(defaultValue = "10") Integer pageSize) {
        return ApiResponse.ok(adminConsoleService.listUsers(keyword, userType, status, page, pageSize));
    }

    @PutMapping("/{userId}/status")
    public ApiResponse<AdminUserResponse> updateStatus(@PathVariable Long userId,
                                                       @Valid @RequestBody AdminUserStatusRequest request) {
        return ApiResponse.ok(adminConsoleService.updateUserStatus(userId, request.getStatus()));
    }
}
