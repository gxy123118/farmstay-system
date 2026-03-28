package com.gxy.mapper;

import com.gxy.model.entity.AiChatSessionRecord;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

@Mapper
public interface AiChatSessionMapper {

    @Insert("INSERT INTO ai_chat_session(user_id, farm_stay_id, scene, title, last_message_at, created_at, updated_at) " +
            "VALUES(#{userId}, #{farmStayId}, #{scene}, #{title}, NOW(), NOW(), NOW())")
    @Options(useGeneratedKeys = true, keyProperty = "id", keyColumn = "id")
    int insert(AiChatSessionRecord record);

    @Select("SELECT * FROM ai_chat_session WHERE id = #{id} LIMIT 1")
    AiChatSessionRecord selectById(@Param("id") Long id);

    @Select("SELECT * FROM ai_chat_session WHERE user_id = #{userId} ORDER BY last_message_at DESC, id DESC")
    List<AiChatSessionRecord> selectByUserId(@Param("userId") Long userId);

    @Update("UPDATE ai_chat_session SET title = #{title}, last_message_at = NOW(), updated_at = NOW() WHERE id = #{id}")
    int updateConversationMeta(@Param("id") Long id, @Param("title") String title);

    @Update("UPDATE ai_chat_session SET title = #{title}, updated_at = NOW() WHERE id = #{id}")
    int updateTitle(@Param("id") Long id, @Param("title") String title);

    @org.apache.ibatis.annotations.Delete("DELETE FROM ai_chat_session WHERE id = #{id}")
    int deleteById(@Param("id") Long id);

    @org.apache.ibatis.annotations.Delete("DELETE FROM ai_chat_session WHERE user_id = #{userId}")
    int deleteByUserId(@Param("userId") Long userId);
}
