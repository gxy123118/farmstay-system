package com.gxy.mapper;

import com.gxy.model.entity.User;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Options;

@Mapper
public interface UserMapper {

    /**
     * 根据账号和角色查询用户，如果是游客或经营者分别传入不同的userType
     */
    @Select("SELECT id, username, password, salt, user_type, display_name, status FROM user_account " +
            "WHERE username = #{username} AND user_type = #{userType} LIMIT 1")
    User selectByUsernameAndType(@Param("username") String username, @Param("userType") String userType);

    @Select("SELECT COUNT(*) FROM user_account WHERE user_type = #{userType} AND status = 'ACTIVE'")
    long countByUserType(@Param("userType") String userType);

    @Insert("INSERT INTO user_account(username, password, salt, display_name, user_type, status, created_at, updated_at) " +
            "VALUES(#{username}, #{password}, #{salt}, #{displayName}, #{userType}, #{status}, NOW(), NOW())")
    @Options(useGeneratedKeys = true, keyProperty = "id", keyColumn = "id")
    int insertUser(User user);
}
