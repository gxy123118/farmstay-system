package com.gxy.model.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;

/**
 * 创建/更新农家乐请求体
 */
@Data
public class FarmStayRequest {

    @NotBlank(message = "农家乐名称不能为空")
    private String name;

    @NotBlank(message = "城市不能为空")
    private String city;

    private String address;

    private String description;

    private String priceRange;

    private String priceLevel;

    private Double averageRating;

    private String coverImage;

    private String contactPhone;

    private String tags;

    private String status;
}
