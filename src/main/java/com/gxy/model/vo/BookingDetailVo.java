package com.gxy.model.vo;

import com.gxy.model.dto.BookingResponse;
import com.gxy.model.dto.FarmStayResponse;
import com.gxy.model.dto.RoomResponse;
import lombok.Data;

/**
 * 对外返回的订单详情，附带房型与农家乐信息。
 */
@Data
public class BookingDetailVo extends BookingResponse {

    private FarmStayResponse farmStay;

    private RoomResponse room;

    /**
     * 是否已提交评价。
     */
    private Boolean reviewed;
}
