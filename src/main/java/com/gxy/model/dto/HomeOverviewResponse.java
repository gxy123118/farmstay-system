package com.gxy.model.dto;

import lombok.Data;

/**
 * 首页汇总数据（提供给前端的统计信息）
 */
@Data
public class HomeOverviewResponse {

    private long publishedFarmstays;

    private long operatorCount;

    private long visitorCount;

    private long dailyOrders;

    private double averageRating;
}
