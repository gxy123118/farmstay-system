package com.gxy.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import com.gxy.common.auth.AuthGuard;
import com.gxy.common.exception.BusinessException;
import com.gxy.mapper.FarmStayMapper;
import com.gxy.model.dto.FarmStayRequest;
import com.gxy.model.dto.FarmStayResponse;
import com.gxy.model.entity.FarmStay;
import com.gxy.service.FarmStayService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class FarmStayServiceImpl implements FarmStayService {

    private final FarmStayMapper farmStayMapper;

    private static final String STATUS_PUBLISHED = "PUBLISHED";
    private static final String STATUS_OFFLINE = "OFFLINE";

    @Override
    public List<FarmStayResponse> list(String city, String keyword, String priceLevel, String tag) {
        List<FarmStay> farmStays = farmStayMapper.selectByConditions(STATUS_PUBLISHED, city, keyword, priceLevel, tag);
        return farmStays.stream().map(this::toResponse).collect(Collectors.toList());
    }

    @Override
    public FarmStayResponse detail(Long id) {
        FarmStay farmStay = farmStayMapper.selectById(id);
        if (farmStay == null) {
            throw new BusinessException("农家乐不存在");
        }
        return toResponse(farmStay);
    }

    @Override
    public List<FarmStayResponse> listByOwner() {
        AuthGuard.enforceOperator();
        Long ownerId = StpUtil.getLoginIdAsLong();
        return farmStayMapper.selectByOwner(ownerId)
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    public FarmStayResponse create(FarmStayRequest request) {
        AuthGuard.enforceOperator();
        FarmStay farmStay = new FarmStay();
        farmStay.setOwnerId(StpUtil.getLoginIdAsLong());
        updateFields(farmStay, request);
        farmStay.setStatus(request.getStatus() == null ? STATUS_PUBLISHED : request.getStatus());
        farmStayMapper.insert(farmStay);
        return toResponse(farmStay);
    }

    @Override
    public FarmStayResponse update(Long id, FarmStayRequest request) {
        AuthGuard.enforceOperator();
        long ownerId = StpUtil.getLoginIdAsLong();
        FarmStay existing = farmStayMapper.selectByIdAndOwner(id, ownerId);
        if (existing == null) {
            throw new BusinessException("只能更新自己的农家乐信息");
        }
        updateFields(existing, request);
        existing.setStatus(request.getStatus() == null ? existing.getStatus() : request.getStatus());
        int changed = farmStayMapper.updateByOwner(existing);
        if (changed == 0) {
            throw new BusinessException("农家乐更新失败");
        }
        return toResponse(existing);
    }

    @Override
    public boolean delete(Long id) {
        AuthGuard.enforceOperator();
        long ownerId = StpUtil.getLoginIdAsLong();
        FarmStay existing = farmStayMapper.selectByIdAndOwner(id, ownerId);
        if (existing == null) {
            throw new BusinessException("只能删除自己的农家乐");
        }
        int changed = farmStayMapper.updateStatusByOwner(id, ownerId, STATUS_OFFLINE);
        if (changed == 0) {
            throw new BusinessException("删除失败");
        }
        return true;
    }

    private void updateFields(FarmStay target, FarmStayRequest request) {
        target.setName(request.getName());
        target.setCity(request.getCity());
        target.setAddress(request.getAddress());
        target.setDescription(request.getDescription());
        target.setPriceRange(request.getPriceRange());
        target.setPriceLevel(request.getPriceLevel());
        target.setAverageRating(request.getAverageRating());
        target.setCoverImage(request.getCoverImage());
        target.setContactPhone(request.getContactPhone());
        target.setTags(request.getTags());
    }

    private FarmStayResponse toResponse(FarmStay farmStay) {
        FarmStayResponse response = new FarmStayResponse();
        response.setId(farmStay.getId());
        response.setOwnerId(farmStay.getOwnerId());
        response.setName(farmStay.getName());
        response.setCity(farmStay.getCity());
        response.setAddress(farmStay.getAddress());
        response.setDescription(farmStay.getDescription());
        response.setPriceRange(farmStay.getPriceRange());
        response.setPriceLevel(farmStay.getPriceLevel());
        response.setAverageRating(farmStay.getAverageRating());
        response.setCoverImage(farmStay.getCoverImage());
        response.setContactPhone(farmStay.getContactPhone());
        response.setTags(farmStay.getTags());
        response.setStatus(farmStay.getStatus());
        response.setCreatedAt(farmStay.getCreatedAt());
        response.setUpdatedAt(farmStay.getUpdatedAt());
        return response;
    }
}
