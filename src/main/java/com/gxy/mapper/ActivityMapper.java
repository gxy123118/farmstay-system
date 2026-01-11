package com.gxy.mapper;

import com.gxy.model.entity.ActivityItem;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

@Mapper
public interface ActivityMapper {

    @Select("SELECT * FROM farmstay_activity WHERE farm_stay_id = #{farmStayId} ORDER BY updated_at DESC")
    List<ActivityItem> selectByFarmStayId(@Param("farmStayId") Long farmStayId);

    @Select("SELECT * FROM farmstay_activity WHERE id = #{id}")
    ActivityItem selectById(@Param("id") Long id);

    @Select("<script>SELECT * FROM farmstay_activity WHERE id IN <foreach collection='ids' item='id' open='(' separator=',' close=')'>#{id}</foreach></script>")
    List<ActivityItem> selectByIds(@Param("ids") List<Long> ids);

    @Insert("INSERT INTO farmstay_activity(farm_stay_id, name, description, schedule, capacity, price, cover_image, tags, status, created_at, updated_at) " +
            "VALUES(#{farmStayId}, #{name}, #{description}, #{schedule}, #{capacity}, #{price}, #{coverImage}, #{tags}, #{status}, NOW(), NOW())")
    @org.apache.ibatis.annotations.Options(useGeneratedKeys = true, keyProperty = "id", keyColumn = "id")
    int insert(ActivityItem item);

    @Update("UPDATE farmstay_activity SET name = #{name}, description = #{description}, schedule = #{schedule}, " +
            "capacity = #{capacity}, price = #{price}, cover_image = #{coverImage}, tags = #{tags}, status = #{status}, updated_at = NOW() " +
            "WHERE id = #{id}")
    int update(ActivityItem item);
}
