package com.gxy.model.dto;

import lombok.Data;

import java.util.Date;

/**
 * 展示给前端的农家乐信息
 */
@Data
public class FarmStayResponse {

    private Long id;

    private Long ownerId;

    private String name;

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

    private Date createdAt;

    private Date updatedAt;
}
