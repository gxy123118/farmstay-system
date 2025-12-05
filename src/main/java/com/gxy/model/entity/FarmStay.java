package com.gxy.model.entity;

import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 农家乐基础信息实体
 */
@Data
public class FarmStay implements Serializable {

    private static final long serialVersionUID = 1L;

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
