package com.gxy.model.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.math.BigDecimal;

/**
 * 房型创建/更新请求
 */
@Data
public class RoomRequest {

    @NotNull(message = "农家乐ID不能为空")
    private Long farmStayId;

    @NotBlank(message = "房型名称不能为空")
    private String name;

    private String description;

    private String bedType;

    private Integer maxGuests;

    @NotNull(message = "价格不能为空")
    private BigDecimal price;

    @NotNull(message = "库存不能为空")
    private Integer stock;

    private String tags;

    private String status;
}
