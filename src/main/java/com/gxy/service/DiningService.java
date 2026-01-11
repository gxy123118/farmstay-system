package com.gxy.service;

import com.gxy.model.dto.DiningRequest;
import com.gxy.model.dto.DiningResponse;

import java.util.List;

public interface DiningService {

    List<DiningResponse> listByFarmStay(Long farmStayId);

    DiningResponse create(DiningRequest request);

    DiningResponse update(Long id, DiningRequest request);
}
