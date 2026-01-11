package com.gxy.model.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.math.BigDecimal;

@Data
public class DiningRequest {

    @NotNull(message = "farmStayId is required")
    private Long farmStayId;

    @NotBlank(message = "name is required")
    private String name;

    private String description;

    @NotNull(message = "price is required")
    private BigDecimal price;

    private String coverImage;

    private String tags;

    private String status;
}
