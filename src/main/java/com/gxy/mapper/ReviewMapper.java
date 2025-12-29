package com.gxy.mapper;

import com.gxy.model.entity.Review;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface ReviewMapper {

    @Insert("INSERT INTO review(order_id, farm_stay_id, visitor_id, rating, content, status, created_at) " +
            "VALUES(#{orderId}, #{farmStayId}, #{visitorId}, #{rating}, #{content}, #{status}, NOW())")
    @Options(useGeneratedKeys = true, keyProperty = "id", keyColumn = "id")
    int insert(Review review);

    @Select("SELECT * FROM review WHERE farm_stay_id = #{farmStayId} AND status = 'APPROVED' ORDER BY created_at DESC")
    List<Review> listApprovedByFarmStay(@Param("farmStayId") Long farmStayId);

    @Select("SELECT * FROM review WHERE id = #{id}")
    Review selectById(@Param("id") Long id);

    @Update("UPDATE review SET status = #{status} WHERE id = #{id}")
    int updateStatus(@Param("id") Long id, @Param("status") String status);
}
