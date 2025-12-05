package com.gxy.service;

import com.gxy.model.dto.FarmStayResponse;
import com.gxy.model.dto.HomeOverviewResponse;

import java.util.List;

public interface HomeService {

    HomeOverviewResponse overview();

    List<FarmStayResponse> recommendations(String city, String priceLevel, String tag);
}
