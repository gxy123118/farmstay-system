package com.gxy.mapper;

import com.gxy.model.entity.UserBalanceFlow;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface UserBalanceFlowMapper {

    @Insert("INSERT INTO user_balance_flow(flow_no, user_id, change_type, biz_no, amount, balance_before, balance_after, remark, created_at) " +
            "VALUES(#{flowNo}, #{userId}, #{changeType}, #{bizNo}, #{amount}, #{balanceBefore}, #{balanceAfter}, #{remark}, NOW())")
    @Options(useGeneratedKeys = true, keyProperty = "id", keyColumn = "id")
    int insert(UserBalanceFlow flow);

    @Select("SELECT * FROM user_balance_flow WHERE user_id = #{userId} ORDER BY created_at DESC, id DESC")
    List<UserBalanceFlow> selectByUserId(@Param("userId") Long userId);
}
