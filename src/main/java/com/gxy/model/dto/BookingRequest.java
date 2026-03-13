package com.gxy.model.dto;

import lombok.Data;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.Date;
import java.util.List;

/**
 * 预订下单请求
 */
@Data
public class BookingRequest {

    @NotNull(message = "farmStayId is required")
    private Long farmStayId;

    @NotNull(message = "roomTypeId is required")
    private Long roomTypeId;

    @NotNull(message = "checkInDate is required")
    @Future(message = "checkInDate must be in the future")
    private Date checkInDate;

    @NotNull(message = "checkOutDate is required")
    @Future(message = "checkOutDate must be in the future")
    private Date checkOutDate;

    @NotNull(message = "guests is required")
    private Integer guests;

    private List<Long> diningItemIds;

    private List<Long> activityItemIds;

    private String couponCode;

    @NotBlank(message = "contactName is required")
    private String contactName;

    @NotBlank(message = "contactPhone is required")
    private String contactPhone;

    private String remarks;
}
