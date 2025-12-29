package com.gxy.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import com.gxy.common.ApiResponse;
import com.gxy.model.dto.FarmStayRequest;
import com.gxy.model.dto.FarmStayResponse;
import com.gxy.service.FarmStayService;
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

/**
 * 农家乐信息对外接口，游客/经营者可查询，经营者可创建/更新
 */
@RestController
@RequestMapping("/api/farmstays")
@Validated
@RequiredArgsConstructor
public class FarmStayController {

    private final FarmStayService farmStayService;

    /**
     * 查询农家乐列表，可按城市/关键词/价格等级/标签筛选
     */
    @GetMapping
    public ApiResponse<List<FarmStayResponse>> list(@RequestParam(required = false) String city,
                                                    @RequestParam(required = false) String keyword,
                                                    @RequestParam(required = false) String priceLevel,
                                                    @RequestParam(required = false) String tag) {
        return ApiResponse.ok(farmStayService.list(city, keyword, priceLevel, tag));
    }

    /**
     * 查询单条农家乐详情
     */
    @GetMapping("/{id}")
    public ApiResponse<FarmStayResponse> detail(@PathVariable Long id) {
        return ApiResponse.ok(farmStayService.detail(id));
    }

    /**
     * 经营者创建农家乐
     */
    @SaCheckLogin
    @PostMapping
    public ApiResponse<FarmStayResponse> create(@Valid @RequestBody FarmStayRequest request) {
        return ApiResponse.ok(farmStayService.create(request));
    }

    /**
     * 经营者更新本身的农家乐信息
     */
    @SaCheckLogin
    @PutMapping("/{id}")
    public ApiResponse<FarmStayResponse> update(@PathVariable Long id, @Valid @RequestBody FarmStayRequest request) {
        return ApiResponse.ok(farmStayService.update(id, request));
    }
}
