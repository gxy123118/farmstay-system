package com.gxy.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import com.gxy.common.auth.AuthGuard;
import com.gxy.common.exception.BusinessException;
import com.gxy.mapper.DiningMapper;
import com.gxy.mapper.FarmStayMapper;
import com.gxy.model.dto.DiningRequest;
import com.gxy.model.dto.DiningResponse;
import com.gxy.model.entity.DiningItem;
import com.gxy.model.entity.FarmStay;
import com.gxy.service.DiningService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DiningServiceImpl implements DiningService {

    private final DiningMapper diningMapper;
    private final FarmStayMapper farmStayMapper;

    @Override
    public List<DiningResponse> listByFarmStay(Long farmStayId) {
        return diningMapper.selectByFarmStayId(farmStayId)
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    public DiningResponse create(DiningRequest request) {
        AuthGuard.enforceOperator();
        ensureOwner(request.getFarmStayId());
        DiningItem item = buildItem(request);
        Date now = new Date();
        item.setCreatedAt(now);
        item.setUpdatedAt(now);
        diningMapper.insert(item);
        return toResponse(item);
    }

    @Override
    public DiningResponse update(Long id, DiningRequest request) {
        AuthGuard.enforceOperator();
        ensureOwner(request.getFarmStayId());
        DiningItem existing = diningMapper.selectById(id);
        if (existing == null) {
            throw new BusinessException("Dining item not found");
        }
        if (!Objects.equals(existing.getFarmStayId(), request.getFarmStayId())) {
            throw new BusinessException("Dining item not in this farmstay");
        }
        DiningItem item = buildItem(request);
        item.setId(id);
        item.setUpdatedAt(new Date());
        int changed = diningMapper.update(item);
        if (changed == 0) {
            throw new BusinessException("Dining update failed");
        }
        return toResponse(item);
    }

    private DiningItem buildItem(DiningRequest request) {
        DiningItem item = new DiningItem();
        item.setFarmStayId(request.getFarmStayId());
        item.setName(request.getName());
        item.setDescription(request.getDescription());
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
            throw new BusinessException("Only owner can manage dining items");
        }
    }

    private DiningResponse toResponse(DiningItem item) {
        DiningResponse response = new DiningResponse();
        response.setId(item.getId());
        response.setFarmStayId(item.getFarmStayId());
        response.setName(item.getName());
        response.setDescription(item.getDescription());
        response.setPrice(item.getPrice());
        response.setCoverImage(item.getCoverImage());
        response.setTags(item.getTags());
        response.setStatus(item.getStatus());
        response.setCreatedAt(item.getCreatedAt());
        response.setUpdatedAt(item.getUpdatedAt());
        return response;
    }
}
