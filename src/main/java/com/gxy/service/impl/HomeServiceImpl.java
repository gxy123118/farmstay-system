package com.gxy.service.impl;

import com.gxy.mapper.FarmStayMapper;
import com.gxy.mapper.UserMapper;
import com.gxy.model.dto.FarmStayResponse;
import com.gxy.model.dto.HomeOverviewResponse;
import com.gxy.model.enums.UserType;
import com.gxy.service.FarmStayService;
import com.gxy.service.HomeService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class HomeServiceImpl implements HomeService {

    private final FarmStayService farmStayService;

    private final FarmStayMapper farmStayMapper;

    private final UserMapper userMapper;

    private static final String STATUS_PUBLISHED = "PUBLISHED";

    @Override
    public HomeOverviewResponse overview() {
        long published = farmStayMapper.countByStatus(STATUS_PUBLISHED);
        long operatorCount = userMapper.countByUserType(UserType.OPERATOR.getCode());
        long visitorCount = userMapper.countByUserType(UserType.VISITOR.getCode());
        Double averageRating = farmStayMapper.averageRating(STATUS_PUBLISHED);
        HomeOverviewResponse response = new HomeOverviewResponse();
        response.setPublishedFarmstays(published);
        response.setOperatorCount(operatorCount);
        response.setVisitorCount(visitorCount);
        response.setDailyOrders(Math.max(80, published * 4));
        response.setAverageRating(averageRating != null ? averageRating : 0.0);
        return response;
    }

    @Override
    public List<FarmStayResponse> recommendations(String city, String priceLevel, String tag) {
        return farmStayService.list(city, null, priceLevel, tag);
    }
}
