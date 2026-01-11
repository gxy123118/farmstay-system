package com.gxy.mapper;

import com.gxy.model.entity.BookingActivityItem;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface BookingActivityMapper {

    @Insert("<script>INSERT INTO booking_order_activity(order_id, activity_item_id, item_name, price, quantity, created_at) VALUES " +
            "<foreach collection='items' item='item' separator=','>(#{item.orderId}, #{item.activityItemId}, #{item.itemName}, #{item.price}, #{item.quantity}, NOW())</foreach></script>")
    int insertBatch(@Param("items") List<BookingActivityItem> items);

    @Select("<script>SELECT id, order_id AS orderId, activity_item_id AS activityItemId, item_name AS itemName, price, quantity, created_at AS createdAt " +
            "FROM booking_order_activity WHERE order_id IN <foreach collection='orderIds' item='orderId' open='(' separator=',' close=')'>#{orderId}</foreach></script>")
    List<BookingActivityItem> selectByOrderIds(@Param("orderIds") List<Long> orderIds);
}
