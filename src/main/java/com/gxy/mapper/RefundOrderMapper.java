package com.gxy.mapper;

import com.gxy.model.entity.RefundOrder;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface RefundOrderMapper {

    @Insert("INSERT INTO refund_order(refund_no, order_id, order_no, user_id, refund_amount, refund_channel, status, reason, created_at, refunded_at, updated_at) " +
            "VALUES(#{refundNo}, #{orderId}, #{orderNo}, #{userId}, #{refundAmount}, #{refundChannel}, #{status}, #{reason}, NOW(), #{refundedAt}, NOW())")
    @Options(useGeneratedKeys = true, keyProperty = "id", keyColumn = "id")
    int insert(RefundOrder refundOrder);

    @Select("SELECT * FROM refund_order WHERE order_id = #{orderId} LIMIT 1")
    RefundOrder selectByOrderId(@Param("orderId") Long orderId);
}
