package com.gxy.mapper;

import com.gxy.model.entity.User;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

@Mapper
public interface UserMapper {

    @Select("SELECT id, username, password, salt, user_type, display_name, status, balance, created_at, updated_at " +
            "FROM user_account WHERE username = #{username} AND user_type = #{userType} LIMIT 1")
    User selectByUsernameAndType(@Param("username") String username, @Param("userType") String userType);

    @Select("SELECT COUNT(*) FROM user_account WHERE user_type = #{userType} AND status = 'ACTIVE'")
    long countByUserType(@Param("userType") String userType);

    @Select("SELECT id, username, user_type, display_name, status, balance, created_at, updated_at " +
            "FROM user_account WHERE id = #{id} LIMIT 1")
    User selectById(@Param("id") Long id);

    @Insert("INSERT INTO user_account(username, password, salt, display_name, user_type, status, created_at, updated_at) " +
            "VALUES(#{username}, #{password}, #{salt}, #{displayName}, #{userType}, #{status}, NOW(), NOW())")
    @Options(useGeneratedKeys = true, keyProperty = "id", keyColumn = "id")
    int insertUser(User user);

    @Select("<script>" +
            "SELECT id, username, display_name, balance FROM user_account WHERE id IN " +
            "<foreach collection='ids' item='id' open='(' separator=',' close=')'>#{id}</foreach>" +
            "</script>")
    List<User> selectByIds(@Param("ids") List<Long> ids);

    @Select("<script>" +
            "SELECT id, username, user_type, display_name, status, balance, created_at, updated_at " +
            "FROM user_account WHERE 1=1 " +
            "<if test=\"userType != null and userType != ''\">AND user_type = #{userType} </if>" +
            "<if test=\"status != null and status != ''\">AND status = #{status} </if>" +
            "<if test=\"keyword != null and keyword != ''\">AND (username LIKE CONCAT('%', #{keyword}, '%') OR display_name LIKE CONCAT('%', #{keyword}, '%')) </if>" +
            "ORDER BY updated_at DESC, id DESC LIMIT #{offset}, #{pageSize}" +
            "</script>")
    List<User> selectPageForAdmin(@Param("keyword") String keyword,
                                  @Param("userType") String userType,
                                  @Param("status") String status,
                                  @Param("offset") int offset,
                                  @Param("pageSize") int pageSize);

    @Select("<script>" +
            "SELECT COUNT(*) FROM user_account WHERE 1=1 " +
            "<if test=\"userType != null and userType != ''\">AND user_type = #{userType} </if>" +
            "<if test=\"status != null and status != ''\">AND status = #{status} </if>" +
            "<if test=\"keyword != null and keyword != ''\">AND (username LIKE CONCAT('%', #{keyword}, '%') OR display_name LIKE CONCAT('%', #{keyword}, '%')) </if>" +
            "</script>")
    long countPageForAdmin(@Param("keyword") String keyword,
                           @Param("userType") String userType,
                           @Param("status") String status);

    @Update("UPDATE user_account SET status = #{status}, updated_at = NOW() WHERE id = #{id}")
    int updateStatus(@Param("id") Long id, @Param("status") String status);
}
