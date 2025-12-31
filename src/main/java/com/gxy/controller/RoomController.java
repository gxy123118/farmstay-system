package com.gxy.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import com.gxy.common.ApiResponse;
import com.gxy.model.dto.RoomRequest;
import com.gxy.model.dto.RoomResponse;
import com.gxy.service.RoomService;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;

/**
 * 房型管理接口
 */
@RestController
@RequestMapping("/api/rooms")
@Validated
@RequiredArgsConstructor
public class RoomController {

    private final RoomService roomService;

    /**
     * 按农家乐查询房型列表
     */
    @GetMapping
    public ApiResponse<List<RoomResponse>> list(@RequestParam Long farmStayId) {
        return ApiResponse.ok(roomService.listByFarmStay(farmStayId));
    }

    /**
     * 经营者创建房型
     */
    @PostMapping
    public ApiResponse<RoomResponse> create(@Valid @RequestBody RoomRequest request) {
        return ApiResponse.ok(roomService.create(request));
    }

    /**
     * 经营者更新房型
     */
    @PutMapping("/{id}")
    public ApiResponse<RoomResponse> update(@PathVariable Long id, @Valid @RequestBody RoomRequest request) {
        return ApiResponse.ok(roomService.update(id, request));
    }
}
