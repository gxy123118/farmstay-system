package com.gxy.mapper;

import com.gxy.model.dto.AdminReviewResponse;
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

    @Select("<script>" +
            "SELECT r.id, r.order_id, r.farm_stay_id, f.name AS farm_stay_name, r.visitor_id, u.username AS visitor_username, " +
            "r.rating, r.content, r.created_at " +
            "FROM review r " +
            "LEFT JOIN farmstay f ON r.farm_stay_id = f.id " +
            "LEFT JOIN user_account u ON r.visitor_id = u.id " +
            "WHERE 1=1 " +
            "<if test=\"keyword != null and keyword != ''\">AND (r.content LIKE CONCAT('%', #{keyword}, '%') OR f.name LIKE CONCAT('%', #{keyword}, '%') OR u.username LIKE CONCAT('%', #{keyword}, '%')) </if>" +
            "ORDER BY r.created_at DESC, r.id DESC LIMIT #{offset}, #{pageSize}" +
            "</script>")
    List<AdminReviewResponse> selectAdminPage(@Param("keyword") String keyword,
                                              @Param("offset") int offset,
                                              @Param("pageSize") int pageSize);

    @Select("<script>" +
            "SELECT COUNT(*) FROM review r " +
            "LEFT JOIN farmstay f ON r.farm_stay_id = f.id " +
            "LEFT JOIN user_account u ON r.visitor_id = u.id " +
            "WHERE 1=1 " +
            "<if test=\"keyword != null and keyword != ''\">AND (r.content LIKE CONCAT('%', #{keyword}, '%') OR f.name LIKE CONCAT('%', #{keyword}, '%') OR u.username LIKE CONCAT('%', #{keyword}, '%')) </if>" +
            "</script>")
    long countAdminPage(@Param("keyword") String keyword);

    @Delete("DELETE FROM review WHERE id = #{id}")
    int deleteById(@Param("id") Long id);
}
