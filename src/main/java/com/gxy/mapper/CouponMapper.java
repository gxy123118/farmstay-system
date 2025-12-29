package com.gxy.mapper;

import com.gxy.model.entity.Coupon;
import org.apache.ibatis.annotations.*;

import java.util.Date;
import java.util.List;

@Mapper
public interface CouponMapper {

    @Insert("INSERT INTO coupon(code, title, description, discount_amount, minimum_spend, valid_from, valid_to, farm_stay_id, total_count, used_count, status, created_at, updated_at) " +
            "VALUES(#{code}, #{title}, #{description}, #{discountAmount}, #{minimumSpend}, #{validFrom}, #{validTo}, #{farmStayId}, #{totalCount}, #{usedCount}, #{status}, NOW(), NOW())")
    @Options(useGeneratedKeys = true, keyProperty = "id", keyColumn = "id")
    int insert(Coupon coupon);

    @Select("SELECT * FROM coupon WHERE code = #{code} LIMIT 1")
    Coupon selectByCode(@Param("code") String code);

    @Select("<script>" +
            "SELECT * FROM coupon WHERE status = 'ACTIVE' " +
            "AND valid_from &lt;= #{now} AND valid_to &gt;= #{now} " +
            "<if test='farmStayId != null'> AND (farm_stay_id IS NULL OR farm_stay_id = #{farmStayId}) </if>" +
            "ORDER BY created_at DESC" +
            "</script>")
    List<Coupon> listAvailable(@Param("now") Date now, @Param("farmStayId") Long farmStayId);

    @Update("UPDATE coupon SET used_count = used_count + 1, updated_at = NOW() WHERE id = #{id} AND used_count &lt; total_count")
    int increaseUsedCount(@Param("id") Long id);
}
