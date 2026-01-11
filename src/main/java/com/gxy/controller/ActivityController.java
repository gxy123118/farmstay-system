 package com.gxy.controller;

import com.gxy.common.ApiResponse;
import com.gxy.model.dto.ActivityRequest;
import com.gxy.model.dto.ActivityResponse;
import com.gxy.service.ActivityService;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;
import java.util.List;

@RestController
@RequestMapping("/api/activities")
@Validated
@RequiredArgsConstructor
public class ActivityController {

    private final ActivityService activityService;

    @GetMapping
    public ApiResponse<List<ActivityResponse>> list(@RequestParam Long farmStayId) {
        return ApiResponse.ok(activityService.listByFarmStay(farmStayId));
    }

    @PostMapping
    public ApiResponse<ActivityResponse> create(@Valid @RequestBody ActivityRequest request) {
        return ApiResponse.ok(activityService.create(request));
    }

    @PutMapping("/{id}")
    public ApiResponse<ActivityResponse> update(@PathVariable Long id, @Valid @RequestBody ActivityRequest request) {
        return ApiResponse.ok(activityService.update(id, request));
    }
}
