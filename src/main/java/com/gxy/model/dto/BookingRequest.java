package com.gxy.model.dto;

import lombok.Data;

import javax.validation.constraints.Future;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.util.Date;

/**
 * 预订下单请求
 */
@Data
public class BookingRequest {

    @NotNull(message = "农家乐ID不能为空")
    private Long farmStayId;

    @NotNull(message = "房型ID不能为空")
    private Long roomTypeId;

    @NotNull(message = "入住日期不能为空")
    @Future(message = "入住日期需大于当前日期")
    private Date checkInDate;

    @NotNull(message = "离店日期不能为空")
    @Future(message = "离店日期需大于当前日期")
    private Date checkOutDate;

    @NotNull(message = "入住人数不能为空")
    private Integer guests;

    private String couponCode;

    @NotBlank(message = "联系人姓名不能为空")
    private String contactName;

    @NotBlank(message = "联系人电话不能为空")
    private String contactPhone;

    private String remarks;
}
