package com.gxy.mapper;

import com.gxy.model.entity.RechargeOrder;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface RechargeOrderMapper {

    @Insert("INSERT INTO recharge_order(recharge_no, user_id, amount, pay_method, status, third_trade_no, subject, notify_content, created_at, paid_at, updated_at) " +
            "VALUES(#{rechargeNo}, #{userId}, #{amount}, #{payMethod}, #{status}, #{thirdTradeNo}, #{subject}, #{notifyContent}, NOW(), #{paidAt}, NOW())")
    @Options(useGeneratedKeys = true, keyProperty = "id", keyColumn = "id")
    int insert(RechargeOrder order);

    @Select("SELECT * FROM recharge_order WHERE recharge_no = #{rechargeNo} LIMIT 1")
    RechargeOrder selectByRechargeNo(@Param("rechargeNo") String rechargeNo);

    @Select("SELECT * FROM recharge_order WHERE recharge_no = #{rechargeNo} AND user_id = #{userId} LIMIT 1")
    RechargeOrder selectByRechargeNoAndUser(@Param("rechargeNo") String rechargeNo, @Param("userId") Long userId);

    @Update("UPDATE recharge_order SET status = 'SUCCESS', third_trade_no = #{thirdTradeNo}, notify_content = #{notifyContent}, paid_at = NOW(), updated_at = NOW() " +
            "WHERE recharge_no = #{rechargeNo} AND status = 'PENDING'")
    int markSuccess(@Param("rechargeNo") String rechargeNo, @Param("thirdTradeNo") String thirdTradeNo, @Param("notifyContent") String notifyContent);

    @Update("UPDATE recharge_order SET status = 'FAILED', notify_content = #{notifyContent}, updated_at = NOW() " +
            "WHERE recharge_no = #{rechargeNo} AND status = 'PENDING'")
    int markFailed(@Param("rechargeNo") String rechargeNo, @Param("notifyContent") String notifyContent);
}
