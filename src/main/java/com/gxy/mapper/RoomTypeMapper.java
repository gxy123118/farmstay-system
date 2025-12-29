package com.gxy.mapper;

import com.gxy.model.entity.RoomType;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface RoomTypeMapper {

    @Select("SELECT * FROM room_type WHERE farm_stay_id = #{farmStayId} ORDER BY updated_at DESC")
    List<RoomType> selectByFarmStayId(@Param("farmStayId") Long farmStayId);

    @Select("SELECT * FROM room_type WHERE id = #{id}")
    RoomType selectById(@Param("id") Long id);

    @Insert("INSERT INTO room_type(farm_stay_id, name, description, bed_type, max_guests, price, stock, tags, status, created_at, updated_at) " +
            "VALUES(#{farmStayId}, #{name}, #{description}, #{bedType}, #{maxGuests}, #{price}, #{stock}, #{tags}, #{status}, NOW(), NOW())")
    @Options(useGeneratedKeys = true, keyProperty = "id", keyColumn = "id")
    int insert(RoomType roomType);

    @Update("UPDATE room_type SET name=#{name}, description=#{description}, bed_type=#{bedType}, max_guests=#{maxGuests}, price=#{price}, " +
            "stock=#{stock}, tags=#{tags}, status=#{status}, updated_at=NOW() WHERE id=#{id} AND farm_stay_id=#{farmStayId}")
    int update(RoomType roomType);
}
