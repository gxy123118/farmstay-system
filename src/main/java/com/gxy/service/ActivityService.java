package com.gxy.service;

import com.gxy.model.dto.ActivityRequest;
import com.gxy.model.dto.ActivityResponse;

import java.util.List;

public interface ActivityService {

    List<ActivityResponse> listByFarmStay(Long farmStayId);

    ActivityResponse create(ActivityRequest request);

    ActivityResponse update(Long id, ActivityRequest request);
}
