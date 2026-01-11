package com.gxy.mapper;

import com.gxy.model.entity.BookingDiningItem;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface BookingDiningMapper {

    @Insert("<script>INSERT INTO booking_order_dining(order_id, dining_item_id, item_name, price, quantity, created_at) VALUES " +
            "<foreach collection='items' item='item' separator=','>(#{item.orderId}, #{item.diningItemId}, #{item.itemName}, #{item.price}, #{item.quantity}, NOW())</foreach></script>")
    int insertBatch(@Param("items") List<BookingDiningItem> items);

    @Select("<script>SELECT id, order_id AS orderId, dining_item_id AS diningItemId, item_name AS itemName, price, quantity, created_at AS createdAt " +
            "FROM booking_order_dining WHERE order_id IN <foreach collection='orderIds' item='orderId' open='(' separator=',' close=')'>#{orderId}</foreach></script>")
    List<BookingDiningItem> selectByOrderIds(@Param("orderIds") List<Long> orderIds);
}
