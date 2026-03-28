package com.gxy.mapper;

import com.gxy.model.entity.UserAccount;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.math.BigDecimal;

@Mapper
public interface UserAccountMapper {

    @Select("SELECT id, username, display_name, user_type, status, balance FROM user_account WHERE id = #{id} LIMIT 1")
    UserAccount selectById(@Param("id") Long id);

    @Update("UPDATE user_account SET balance = balance + #{amount}, updated_at = NOW() WHERE id = #{id}")
    int increaseBalance(@Param("id") Long id, @Param("amount") BigDecimal amount);

    @Update("UPDATE user_account SET balance = balance - #{amount}, updated_at = NOW() WHERE id = #{id} AND balance >= #{amount}")
    int decreaseBalance(@Param("id") Long id, @Param("amount") BigDecimal amount);
}
