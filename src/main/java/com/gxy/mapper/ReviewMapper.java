package com.gxy.mapper;

import com.gxy.model.entity.Review;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface ReviewMapper {

    @Insert("INSERT INTO review(order_id, farm_stay_id, visitor_id, rating, content, created_at) " +
            "VALUES(#{orderId}, #{farmStayId}, #{visitorId}, #{rating}, #{content}, NOW())")
    @Options(useGeneratedKeys = true, keyProperty = "id", keyColumn = "id")
    int insert(Review review);

    @Select("SELECT * FROM review WHERE farm_stay_id = #{farmStayId} ORDER BY created_at DESC")
    List<Review> listByFarmStay(@Param("farmStayId") Long farmStayId);

    @Select("SELECT * FROM review WHERE id = #{id}")
    Review selectById(@Param("id") Long id);

    @Select("SELECT * FROM review WHERE order_id = #{orderId} LIMIT 1")
    Review selectByOrderId(@Param("orderId") Long orderId);

    @Select("<script>" +
            "SELECT * FROM review WHERE order_id IN " +
            "<foreach collection='orderIds' item='id' open='(' separator=',' close=')'>#{id}</foreach>" +
            "</script>")
    List<Review> selectByOrderIds(@Param("orderIds") List<Long> orderIds);

    @Update("UPDATE review SET rating = #{rating}, content = #{content} WHERE order_id = #{orderId} AND visitor_id = #{visitorId}")
    int updateByOrder(@Param("orderId") Long orderId, @Param("visitorId") Long visitorId, @Param("rating") Integer rating, @Param("content") String content);
}
