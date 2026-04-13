package com.gxy.model.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Data
public class FarmStayResourceSaveRequest {

    @NotBlank(message = "name is required")
    private String name;

    @NotBlank(message = "city is required")
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

    @Valid
    private List<RoomPayload> rooms = new ArrayList<>();

    @Valid
    private List<DiningPayload> dinings = new ArrayList<>();

    @Valid
    private List<ActivityPayload> activities = new ArrayList<>();

    @Data
    public static class RoomPayload {
        private Long id;

        @NotBlank(message = "room name is required")
        private String name;

        private String description;

        private String bedType;

        private Integer maxGuests;

        @NotNull(message = "room price is required")
        private BigDecimal price;

        @NotNull(message = "room stock is required")
        private Integer stock;

        private String tags;

        private String status;
    }

    @Data
    public static class DiningPayload {
        private Long id;

        @NotBlank(message = "dining name is required")
        private String name;

        private String description;

        @NotNull(message = "dining price is required")
        private BigDecimal price;

        private String coverImage;

        private String tags;

        private String status;
    }

    @Data
    public static class ActivityPayload {
        private Long id;

        @NotBlank(message = "activity name is required")
        private String name;

        private String description;

        private String schedule;

        private Integer capacity;

        @NotNull(message = "activity price is required")
        private BigDecimal price;

        private String coverImage;

        private String tags;

        private String status;
    }
}

