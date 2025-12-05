package com.gxy.controller;

import com.gxy.common.ApiResponse;
import com.gxy.model.dto.FarmStayResponse;
import com.gxy.model.dto.HomeOverviewResponse;
import com.gxy.service.HomeService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 首页展示所需的聚合数据接口
 */
@RestController
@RequestMapping("/api/home")
@RequiredArgsConstructor
public class HomeController {

    private final HomeService homeService;

    @GetMapping("/overview")
    public ApiResponse<HomeOverviewResponse> overview() {
        return ApiResponse.ok(homeService.overview());
    }

    @GetMapping("/recommendations")
    public ApiResponse<List<FarmStayResponse>> recommendations(@RequestParam(required = false) String city,
                                                               @RequestParam(required = false) String priceLevel,
                                                               @RequestParam(required = false) String tag) {
        return ApiResponse.ok(homeService.recommendations(city, priceLevel, tag));
    }
}
