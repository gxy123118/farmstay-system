package com.gxy.service;

import com.gxy.model.dto.RoomRequest;
import com.gxy.model.dto.RoomResponse;

import java.util.List;

public interface RoomService {

    List<RoomResponse> listByFarmStay(Long farmStayId);

    RoomResponse create(RoomRequest request);

    RoomResponse update(Long id, RoomRequest request);
}
