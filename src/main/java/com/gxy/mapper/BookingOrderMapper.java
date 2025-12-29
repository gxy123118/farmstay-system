package com.gxy.mapper;

import com.gxy.model.entity.BookingOrder;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface BookingOrderMapper {

    @Insert("INSERT INTO booking_order(order_no, visitor_id, farm_stay_id, room_type_id, check_in_date, check_out_date, guests, total_amount, status, payment_channel, contact_name, contact_phone, coupon_code, remarks, created_at, updated_at) " +
            "VALUES(#{orderNo}, #{visitorId}, #{farmStayId}, #{roomTypeId}, #{checkInDate}, #{checkOutDate}, #{guests}, #{totalAmount}, #{status}, #{paymentChannel}, #{contactName}, #{contactPhone}, #{couponCode}, #{remarks}, NOW(), NOW())")
    @Options(useGeneratedKeys = true, keyProperty = "id", keyColumn = "id")
    int insert(BookingOrder order);

    @Select("SELECT * FROM booking_order WHERE id = #{id}")
    BookingOrder selectById(@Param("id") Long id);

    @Select("SELECT * FROM booking_order WHERE visitor_id = #{visitorId} ORDER BY created_at DESC")
    List<BookingOrder> selectByVisitor(@Param("visitorId") Long visitorId);

    @Select("SELECT * FROM booking_order WHERE farm_stay_id = #{farmStayId} ORDER BY created_at DESC")
    List<BookingOrder> selectByFarmStay(@Param("farmStayId") Long farmStayId);

    @Update("UPDATE booking_order SET status=#{status}, payment_channel=#{paymentChannel}, updated_at=NOW() WHERE id=#{id}")
    int updateStatus(@Param("id") Long id, @Param("status") String status, @Param("paymentChannel") String paymentChannel);

    @Update("UPDATE booking_order SET status=#{status}, updated_at=NOW() WHERE id=#{id} AND visitor_id=#{visitorId}")
    int updateStatusByVisitor(@Param("id") Long id, @Param("visitorId") Long visitorId, @Param("status") String status);
}
