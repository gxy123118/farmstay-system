package com.gxy.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import com.gxy.common.auth.AuthGuard;
import com.gxy.common.exception.BusinessException;
import com.gxy.mapper.ActivityMapper;
import com.gxy.mapper.FarmStayMapper;
import com.gxy.model.dto.ActivityRequest;
import com.gxy.model.dto.ActivityResponse;
import com.gxy.model.entity.ActivityItem;
import com.gxy.model.entity.FarmStay;
import com.gxy.service.ActivityService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ActivityServiceImpl implements ActivityService {

    private final ActivityMapper activityMapper;
    private final FarmStayMapper farmStayMapper;

    @Override
    public List<ActivityResponse> listByFarmStay(Long farmStayId) {
        return activityMapper.selectByFarmStayId(farmStayId)
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    public ActivityResponse create(ActivityRequest request) {
        AuthGuard.enforceOperator();
        ensureOwner(request.getFarmStayId());
        ActivityItem item = buildItem(request);
        Date now = new Date();
        item.setCreatedAt(now);
        item.setUpdatedAt(now);
        activityMapper.insert(item);
        return toResponse(item);
    }

    @Override
    public ActivityResponse update(Long id, ActivityRequest request) {
        AuthGuard.enforceOperator();
        ensureOwner(request.getFarmStayId());
        ActivityItem existing = activityMapper.selectById(id);
        if (existing == null) {
            throw new BusinessException("Activity not found");
        }
        if (!Objects.equals(existing.getFarmStayId(), request.getFarmStayId())) {
            throw new BusinessException("Activity not in this farmstay");
        }
        ActivityItem item = buildItem(request);
        item.setId(id);
        item.setUpdatedAt(new Date());
        int changed = activityMapper.update(item);
        if (changed == 0) {
            throw new BusinessException("Activity update failed");
        }
        return toResponse(item);
    }

    private ActivityItem buildItem(ActivityRequest request) {
        ActivityItem item = new ActivityItem();
        item.setFarmStayId(request.getFarmStayId());
        item.setName(request.getName());
        item.setDescription(request.getDescription());
        item.setSchedule(request.getSchedule());
        item.setCapacity(request.getCapacity());
        item.setPrice(request.getPrice());
        item.setCoverImage(request.getCoverImage());
        item.setTags(request.getTags());
        item.setStatus(request.getStatus() == null ? "ACTIVE" : request.getStatus());
        return item;
    }

    private void ensureOwner(Long farmStayId) {
        Long ownerId = StpUtil.getLoginIdAsLong();
        FarmStay farmStay = farmStayMapper.selectByIdAndOwner(farmStayId, ownerId);
        if (farmStay == null) {
            throw new BusinessException("Only owner can manage activities");
        }
    }

    private ActivityResponse toResponse(ActivityItem item) {
        ActivityResponse response = new ActivityResponse();
        response.setId(item.getId());
        response.setFarmStayId(item.getFarmStayId());
        response.setName(item.getName());
        response.setDescription(item.getDescription());
        response.setSchedule(item.getSchedule());
        response.setCapacity(item.getCapacity());
        response.setPrice(item.getPrice());
        response.setCoverImage(item.getCoverImage());
        response.setTags(item.getTags());
        response.setStatus(item.getStatus());
        response.setCreatedAt(item.getCreatedAt());
        response.setUpdatedAt(item.getUpdatedAt());
        return response;
    }
}
