package com.gxy.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import com.gxy.common.PageResponse;
import com.gxy.common.auth.AuthGuard;
import com.gxy.common.exception.BusinessException;
import com.gxy.mapper.ActivityMapper;
import com.gxy.mapper.DiningMapper;
import com.gxy.mapper.FarmStayMapper;
import com.gxy.mapper.RoomTypeMapper;
import com.gxy.model.dto.ActivityResponse;
import com.gxy.model.dto.DiningResponse;
import com.gxy.model.dto.FarmStayRequest;
import com.gxy.model.dto.FarmStayResourceSaveRequest;
import com.gxy.model.dto.FarmStayResourceSaveResponse;
import com.gxy.model.dto.FarmStayResponse;
import com.gxy.model.dto.RoomResponse;
import com.gxy.model.entity.ActivityItem;
import com.gxy.model.entity.DiningItem;
import com.gxy.model.entity.FarmStay;
import com.gxy.model.entity.RoomType;
import com.gxy.service.FarmStayService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class FarmStayServiceImpl implements FarmStayService {

    private final FarmStayMapper farmStayMapper;
    private final RoomTypeMapper roomTypeMapper;
    private final DiningMapper diningMapper;
    private final ActivityMapper activityMapper;

    private static final String STATUS_PUBLISHED = "PUBLISHED";
    private static final String STATUS_OFFLINE = "OFFLINE";

    @Override
    public PageResponse<FarmStayResponse> list(String city, String keyword, String priceLevel, String tag, Integer page, Integer pageSize) {
        int currentPage = page == null || page < 1 ? 1 : page;
        int currentPageSize = pageSize == null || pageSize < 1 ? 8 : Math.min(pageSize, 50);
        int offset = (currentPage - 1) * currentPageSize;

        List<FarmStay> farmStays = farmStayMapper.selectPageByConditions(
                STATUS_PUBLISHED,
                city,
                keyword,
                priceLevel,
                tag,
                offset,
                currentPageSize
        );
        long total = farmStayMapper.countByConditions(STATUS_PUBLISHED, city, keyword, priceLevel, tag);

        return PageResponse.of(
                farmStays.stream().map(this::toResponse).collect(Collectors.toList()),
                total,
                currentPage,
                currentPageSize
        );
    }

    @Override
    public FarmStayResponse detail(Long id) {
        FarmStay farmStay = farmStayMapper.selectById(id);
        if (farmStay == null) {
            throw new BusinessException("Farmstay not found");
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
            throw new BusinessException("Only owner can update this farmstay");
        }
        updateFields(existing, request);
        existing.setStatus(request.getStatus() == null ? existing.getStatus() : request.getStatus());
        int changed = farmStayMapper.updateByOwner(existing);
        if (changed == 0) {
            throw new BusinessException("Farmstay update failed");
        }
        return toResponse(existing);
    }

    @Override
    public FarmStayResponse offline(Long id) {
        AuthGuard.enforceOperator();
        long ownerId = StpUtil.getLoginIdAsLong();
        FarmStay existing = farmStayMapper.selectByIdAndOwner(id, ownerId);
        if (existing == null) {
            throw new BusinessException("Only owner can update this farmstay");
        }
        int changed = farmStayMapper.updateStatusByOwner(id, ownerId, STATUS_OFFLINE);
        if (changed == 0) {
            throw new BusinessException("Farmstay offline failed");
        }
        FarmStay updated = farmStayMapper.selectById(id);
        return toResponse(updated);
    }

    @Override
    public FarmStayResponse publish(Long id) {
        AuthGuard.enforceOperator();
        long ownerId = StpUtil.getLoginIdAsLong();
        FarmStay existing = farmStayMapper.selectByIdAndOwner(id, ownerId);
        if (existing == null) {
            throw new BusinessException("Only owner can update this farmstay");
        }
        int changed = farmStayMapper.updateStatusByOwner(id, ownerId, STATUS_PUBLISHED);
        if (changed == 0) {
            throw new BusinessException("Farmstay publish failed");
        }
        FarmStay updated = farmStayMapper.selectById(id);
        return toResponse(updated);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public FarmStayResourceSaveResponse saveResources(Long id, FarmStayResourceSaveRequest request) {
        AuthGuard.enforceOperator();
        long ownerId = StpUtil.getLoginIdAsLong();
        FarmStay existing = farmStayMapper.selectByIdAndOwner(id, ownerId);
        if (existing == null) {
            throw new BusinessException("Only owner can update this farmstay");
        }

        updateFields(existing, request);
        existing.setStatus(request.getStatus() == null ? existing.getStatus() : request.getStatus());
        int changed = farmStayMapper.updateByOwner(existing);
        if (changed == 0) {
            throw new BusinessException("Farmstay update failed");
        }

        syncRooms(existing.getId(), request.getRooms());
        syncDinings(existing.getId(), request.getDinings());
        syncActivities(existing.getId(), request.getActivities());

        FarmStayResourceSaveResponse response = new FarmStayResourceSaveResponse();
        response.setFarmStay(toResponse(farmStayMapper.selectById(existing.getId())));
        response.setRooms(roomTypeMapper.selectByFarmStayId(existing.getId()).stream().map(this::toRoomResponse).collect(Collectors.toList()));
        response.setDinings(diningMapper.selectByFarmStayId(existing.getId()).stream().map(this::toDiningResponse).collect(Collectors.toList()));
        response.setActivities(activityMapper.selectByFarmStayId(existing.getId()).stream().map(this::toActivityResponse).collect(Collectors.toList()));
        return response;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean delete(Long id) {
        AuthGuard.enforceOperator();
        long ownerId = StpUtil.getLoginIdAsLong();
        FarmStay existing = farmStayMapper.selectByIdAndOwner(id, ownerId);
        if (existing == null) {
            throw new BusinessException("Only owner can delete this farmstay");
        }
        roomTypeMapper.deleteByFarmStayId(id);
        diningMapper.deleteByFarmStayId(id);
        activityMapper.deleteByFarmStayId(id);
        int changed = farmStayMapper.deleteByIdAndOwner(id, ownerId);
        if (changed == 0) {
            throw new BusinessException("Delete failed");
        }
        return true;
    }

    private void syncRooms(Long farmStayId, List<FarmStayResourceSaveRequest.RoomPayload> payloads) {
        List<RoomType> existing = roomTypeMapper.selectByFarmStayId(farmStayId);
        Map<Long, RoomType> existingById = existing.stream()
                .collect(Collectors.toMap(RoomType::getId, Function.identity()));
        Set<Long> keptIds = new HashSet<>();
        Date now = new Date();

        for (FarmStayResourceSaveRequest.RoomPayload payload : payloads) {
            RoomType room = new RoomType();
            room.setFarmStayId(farmStayId);
            room.setName(payload.getName());
            room.setDescription(payload.getDescription());
            room.setBedType(payload.getBedType());
            room.setMaxGuests(payload.getMaxGuests());
            room.setPrice(payload.getPrice());
            room.setStock(payload.getStock());
            room.setTags(payload.getTags());
            room.setStatus(payload.getStatus() == null ? "ACTIVE" : payload.getStatus());
            room.setUpdatedAt(now);

            if (payload.getId() == null) {
                room.setCreatedAt(now);
                roomTypeMapper.insert(room);
                continue;
            }

            RoomType old = existingById.get(payload.getId());
            if (old == null) {
                throw new BusinessException("Room does not belong to current farmstay");
            }
            room.setId(payload.getId());
            int changed = roomTypeMapper.update(room);
            if (changed == 0) {
                throw new BusinessException("Room update failed");
            }
            keptIds.add(payload.getId());
        }

        for (RoomType old : existing) {
            if (!keptIds.contains(old.getId())) {
                roomTypeMapper.deleteByIdAndFarmStay(old.getId(), farmStayId);
            }
        }
    }

    private void syncDinings(Long farmStayId, List<FarmStayResourceSaveRequest.DiningPayload> payloads) {
        List<DiningItem> existing = diningMapper.selectByFarmStayId(farmStayId);
        Map<Long, DiningItem> existingById = existing.stream()
                .collect(Collectors.toMap(DiningItem::getId, Function.identity()));
        Set<Long> keptIds = new HashSet<>();
        Date now = new Date();

        for (FarmStayResourceSaveRequest.DiningPayload payload : payloads) {
            DiningItem item = new DiningItem();
            item.setFarmStayId(farmStayId);
            item.setName(payload.getName());
            item.setDescription(payload.getDescription());
            item.setPrice(payload.getPrice());
            item.setCoverImage(payload.getCoverImage());
            item.setTags(payload.getTags());
            item.setStatus(payload.getStatus() == null ? "ACTIVE" : payload.getStatus());
            item.setUpdatedAt(now);

            if (payload.getId() == null) {
                item.setCreatedAt(now);
                diningMapper.insert(item);
                continue;
            }

            DiningItem old = existingById.get(payload.getId());
            if (old == null) {
                throw new BusinessException("Dining does not belong to current farmstay");
            }
            item.setId(payload.getId());
            int changed = diningMapper.update(item);
            if (changed == 0) {
                throw new BusinessException("Dining update failed");
            }
            keptIds.add(payload.getId());
        }

        for (DiningItem old : existing) {
            if (!keptIds.contains(old.getId())) {
                diningMapper.deleteByIdAndFarmStay(old.getId(), farmStayId);
            }
        }
    }

    private void syncActivities(Long farmStayId, List<FarmStayResourceSaveRequest.ActivityPayload> payloads) {
        List<ActivityItem> existing = activityMapper.selectByFarmStayId(farmStayId);
        Map<Long, ActivityItem> existingById = existing.stream()
                .collect(Collectors.toMap(ActivityItem::getId, Function.identity()));
        Set<Long> keptIds = new HashSet<>();
        Date now = new Date();

        for (FarmStayResourceSaveRequest.ActivityPayload payload : payloads) {
            ActivityItem item = new ActivityItem();
            item.setFarmStayId(farmStayId);
            item.setName(payload.getName());
            item.setDescription(payload.getDescription());
            item.setSchedule(payload.getSchedule());
            item.setCapacity(payload.getCapacity());
            item.setPrice(payload.getPrice());
            item.setCoverImage(payload.getCoverImage());
            item.setTags(payload.getTags());
            item.setStatus(payload.getStatus() == null ? "ACTIVE" : payload.getStatus());
            item.setUpdatedAt(now);

            if (payload.getId() == null) {
                item.setCreatedAt(now);
                activityMapper.insert(item);
                continue;
            }

            ActivityItem old = existingById.get(payload.getId());
            if (old == null) {
                throw new BusinessException("Activity does not belong to current farmstay");
            }
            item.setId(payload.getId());
            int changed = activityMapper.update(item);
            if (changed == 0) {
                throw new BusinessException("Activity update failed");
            }
            keptIds.add(payload.getId());
        }

        for (ActivityItem old : existing) {
            if (!keptIds.contains(old.getId())) {
                activityMapper.deleteByIdAndFarmStay(old.getId(), farmStayId);
            }
        }
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

    private void updateFields(FarmStay target, FarmStayResourceSaveRequest request) {
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

    private RoomResponse toRoomResponse(RoomType roomType) {
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

    private DiningResponse toDiningResponse(DiningItem item) {
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

    private ActivityResponse toActivityResponse(ActivityItem item) {
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
