package com.gxy.controller;

import com.gxy.common.ApiResponse;
import com.gxy.model.dto.DiningRequest;
import com.gxy.model.dto.DiningResponse;
import com.gxy.service.DiningService;
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
@RequestMapping("/api/dinings")
@Validated
@RequiredArgsConstructor
public class DiningController {

    private final DiningService diningService;

    @GetMapping
    public ApiResponse<List<DiningResponse>> list(@RequestParam Long farmStayId) {
        return ApiResponse.ok(diningService.listByFarmStay(farmStayId));
    }

    @PostMapping
    public ApiResponse<DiningResponse> create(@Valid @RequestBody DiningRequest request) {
        return ApiResponse.ok(diningService.create(request));
    }

    @PutMapping("/{id}")
    public ApiResponse<DiningResponse> update(@PathVariable Long id, @Valid @RequestBody DiningRequest request) {
        return ApiResponse.ok(diningService.update(id, request));
    }
}
