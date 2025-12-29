package com.gxy.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import com.gxy.common.exception.BusinessException;
import com.gxy.mapper.FarmStayMapper;
import com.gxy.mapper.RoomTypeMapper;
import com.gxy.model.dto.RoomRequest;
import com.gxy.model.dto.RoomResponse;
import com.gxy.model.entity.FarmStay;
import com.gxy.model.entity.RoomType;
import com.gxy.model.enums.UserType;
import com.gxy.service.RoomService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RoomServiceImpl implements RoomService {

    private final RoomTypeMapper roomTypeMapper;
    private final FarmStayMapper farmStayMapper;

    @Override
    public List<RoomResponse> listByFarmStay(Long farmStayId) {
        return roomTypeMapper.selectByFarmStayId(farmStayId)
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    public RoomResponse create(RoomRequest request) {
        enforceOperator();
        ensureOwner(request.getFarmStayId());
        RoomType roomType = buildRoomType(request);
        roomType.setCreatedAt(new java.util.Date());
        roomType.setUpdatedAt(roomType.getCreatedAt());
        roomTypeMapper.insert(roomType);
        return toResponse(roomType);
    }

    @Override
    public RoomResponse update(Long id, RoomRequest request) {
        enforceOperator();
        ensureOwner(request.getFarmStayId());
        RoomType exists = roomTypeMapper.selectById(id);
        if (exists == null) {
            throw new BusinessException("房型不存在");
        }
        if (!Objects.equals(exists.getFarmStayId(), request.getFarmStayId())) {
            throw new BusinessException("房型不属于当前农家乐");
        }
        RoomType roomType = buildRoomType(request);
        roomType.setId(id);
        roomType.setUpdatedAt(new java.util.Date());
        int changed = roomTypeMapper.update(roomType);
        if (changed == 0) {
            throw new BusinessException("房型更新失败");
        }
        return toResponse(roomType);
    }

    private RoomType buildRoomType(RoomRequest request) {
        RoomType roomType = new RoomType();
        roomType.setFarmStayId(request.getFarmStayId());
        roomType.setName(request.getName());
        roomType.setDescription(request.getDescription());
        roomType.setBedType(request.getBedType());
        roomType.setMaxGuests(request.getMaxGuests());
        roomType.setPrice(request.getPrice());
        roomType.setStock(request.getStock());
        roomType.setTags(request.getTags());
        roomType.setStatus(request.getStatus() == null ? "ACTIVE" : request.getStatus());
        return roomType;
    }

    private void ensureOwner(Long farmStayId) {
        Long ownerId = StpUtil.getLoginIdAsLong();
        FarmStay farmStay = farmStayMapper.selectByIdAndOwner(farmStayId, ownerId);
        if (farmStay == null) {
            throw new BusinessException("仅能管理自己的农家乐房型");
        }
    }

    private void enforceOperator() {
        if (!Objects.equals(UserType.OPERATOR.getCode(), StpUtil.getLoginType())) {
            throw new BusinessException("此操作仅限经营者");
        }
    }

    private RoomResponse toResponse(RoomType roomType) {
        RoomResponse response = new RoomResponse();
        response.setId(roomType.getId());
        response.setFarmStayId(roomType.getFarmStayId());
        response.setName(roomType.getName());
        response.setDescription(roomType.getDescription());
        response.setBedType(roomType.getBedType());
        response.setMaxGuests(roomType.getMaxGuests());
        response.setPrice(roomType.getPrice());
        response.setStock(roomType.getStock());
        response.setTags(roomType.getTags());
        response.setStatus(roomType.getStatus());
        response.setCreatedAt(roomType.getCreatedAt());
        response.setUpdatedAt(roomType.getUpdatedAt());
        return response;
    }
}
