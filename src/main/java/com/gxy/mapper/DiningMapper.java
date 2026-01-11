package com.gxy.mapper;

import com.gxy.model.entity.DiningItem;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

@Mapper
public interface DiningMapper {

    @Select("SELECT * FROM farmstay_dining WHERE farm_stay_id = #{farmStayId} ORDER BY updated_at DESC")
    List<DiningItem> selectByFarmStayId(@Param("farmStayId") Long farmStayId);

    @Select("SELECT * FROM farmstay_dining WHERE id = #{id}")
    DiningItem selectById(@Param("id") Long id);

    @Select("<script>SELECT * FROM farmstay_dining WHERE id IN <foreach collection='ids' item='id' open='(' separator=',' close=')'>#{id}</foreach></script>")
    List<DiningItem> selectByIds(@Param("ids") List<Long> ids);

    @Insert("INSERT INTO farmstay_dining(farm_stay_id, name, description, price, cover_image, tags, status, created_at, updated_at) " +
            "VALUES(#{farmStayId}, #{name}, #{description}, #{price}, #{coverImage}, #{tags}, #{status}, NOW(), NOW())")
    @org.apache.ibatis.annotations.Options(useGeneratedKeys = true, keyProperty = "id", keyColumn = "id")
    int insert(DiningItem item);

    @Update("UPDATE farmstay_dining SET name = #{name}, description = #{description}, price = #{price}, " +
            "cover_image = #{coverImage}, tags = #{tags}, status = #{status}, updated_at = NOW() WHERE id = #{id}")
    int update(DiningItem item);
}
