package com.gxy.mapper;

import com.gxy.model.entity.WithdrawOrder;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

@Mapper
public interface WithdrawOrderMapper {

    @Insert("INSERT INTO withdraw_order(withdraw_no, user_id, amount, channel, account_name, account_no, status, remark, review_remark, transfer_no, created_at, reviewed_at, paid_at, updated_at) " +
            "VALUES(#{withdrawNo}, #{userId}, #{amount}, #{channel}, #{accountName}, #{accountNo}, #{status}, #{remark}, #{reviewRemark}, #{transferNo}, NOW(), #{reviewedAt}, #{paidAt}, NOW())")
    @Options(useGeneratedKeys = true, keyProperty = "id", keyColumn = "id")
    int insert(WithdrawOrder order);

    @Select("SELECT * FROM withdraw_order WHERE id = #{id}")
    WithdrawOrder selectById(@Param("id") Long id);

    @Select("SELECT * FROM withdraw_order WHERE user_id = #{userId} ORDER BY created_at DESC, id DESC")
    List<WithdrawOrder> selectByUserId(@Param("userId") Long userId);

    @Select("<script>" +
            "SELECT * FROM withdraw_order WHERE 1=1 " +
            "<if test=\"status != null and status != ''\">AND status = #{status} </if>" +
            "ORDER BY created_at DESC, id DESC LIMIT #{offset}, #{pageSize}" +
            "</script>")
    List<WithdrawOrder> selectAdminPage(@Param("status") String status,
                                        @Param("offset") int offset,
                                        @Param("pageSize") int pageSize);

    @Select("<script>" +
            "SELECT COUNT(*) FROM withdraw_order WHERE 1=1 " +
            "<if test=\"status != null and status != ''\">AND status = #{status} </if>" +
            "</script>")
    long countAdminPage(@Param("status") String status);

    @Update("UPDATE withdraw_order SET status = #{targetStatus}, review_remark = #{reviewRemark}, reviewed_at = NOW(), updated_at = NOW() " +
            "WHERE id = #{id} AND status = #{currentStatus}")
    int updateReviewStatus(@Param("id") Long id,
                           @Param("currentStatus") String currentStatus,
                           @Param("targetStatus") String targetStatus,
                           @Param("reviewRemark") String reviewRemark);

    @Update("UPDATE withdraw_order SET status = 'SUCCESS', transfer_no = #{transferNo}, review_remark = #{reviewRemark}, paid_at = NOW(), updated_at = NOW() " +
            "WHERE id = #{id} AND status = 'APPROVED'")
    int markSuccess(@Param("id") Long id,
                    @Param("transferNo") String transferNo,
                    @Param("reviewRemark") String reviewRemark);
}
