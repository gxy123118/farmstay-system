package com.gxy.mapper;

import com.gxy.model.entity.FarmStay;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

@Mapper
public interface FarmStayMapper {

    @Select("<script>" +
            "SELECT * FROM farmstay " +
            "WHERE status = #{status} " +
            "<if test=\"city != null and city != ''\">AND city LIKE CONCAT('%', #{city}, '%')</if> " +
            "<if test=\"keyword != null and keyword != ''\">AND (name LIKE CONCAT('%', #{keyword}, '%') OR tags LIKE CONCAT('%', #{keyword}, '%'))</if> " +
            "<if test=\"tag != null and tag != ''\">AND tags LIKE CONCAT('%', #{tag}, '%')</if> " +
            "<if test=\"priceLevel != null and priceLevel != ''\">AND price_level = #{priceLevel}</if> " +
            "ORDER BY updated_at DESC " +
            "</script>")
    List<FarmStay> selectByConditions(@Param("status") String status,
                                      @Param("city") String city,
                                      @Param("keyword") String keyword,
                                      @Param("priceLevel") String priceLevel,
                                      @Param("tag") String tag);

    @Select("SELECT COUNT(*) FROM farmstay WHERE status = #{status}")
    long countByStatus(@Param("status") String status);

    @Select("SELECT AVG(average_rating) FROM farmstay WHERE status = #{status}")
    Double averageRating(@Param("status") String status);

    @Select("SELECT * FROM farmstay WHERE id = #{id}")
    FarmStay selectById(@Param("id") Long id);

    @Select("SELECT * FROM farmstay WHERE id = #{id} AND owner_id = #{ownerId}")
    FarmStay selectByIdAndOwner(@Param("id") Long id, @Param("ownerId") Long ownerId);

    @Select("SELECT * FROM farmstay WHERE owner_id = #{ownerId} ORDER BY updated_at DESC")
    List<FarmStay> selectByOwner(@Param("ownerId") Long ownerId);

    @Insert("INSERT INTO farmstay(owner_id, name, city, address, description, price_range, price_level, average_rating, cover_image, contact_phone, tags, status, created_at, updated_at) " +
            "VALUES(#{ownerId}, #{name}, #{city}, #{address}, #{description}, #{priceRange}, #{priceLevel}, #{averageRating}, #{coverImage}, #{contactPhone}, #{tags}, #{status}, NOW(), NOW())")
    @org.apache.ibatis.annotations.Options(useGeneratedKeys = true, keyProperty = "id", keyColumn = "id")
    int insert(FarmStay farmStay);

    @Update("UPDATE farmstay SET " +
            "name = #{name}, city = #{city}, address = #{address}, description = #{description}, " +
            "price_range = #{priceRange}, price_level = #{priceLevel}, average_rating = #{averageRating}, " +
            "cover_image = #{coverImage}, contact_phone = #{contactPhone}, tags = #{tags}, status = #{status}, updated_at = NOW() " +
            "WHERE id = #{id} AND owner_id = #{ownerId}")
    int updateByOwner(FarmStay farmStay);

    @Update("UPDATE farmstay SET status = #{status}, updated_at = NOW() WHERE id = #{id} AND owner_id = #{ownerId}")
    int updateStatusByOwner(@Param("id") Long id, @Param("ownerId") Long ownerId, @Param("status") String status);
}
