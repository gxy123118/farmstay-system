package com.gxy.mapper;

import com.gxy.model.entity.AiChatMessageRecord;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

@Mapper
public interface AiChatMessageMapper {

    @Insert("INSERT INTO ai_chat_message(session_id, role, content, citations_json, confidence, refuse_reason, fallback, useful, feedback_comment, created_at, updated_at) " +
            "VALUES(#{sessionId}, #{role}, #{content}, #{citationsJson}, #{confidence}, #{refuseReason}, #{fallback}, #{useful}, #{feedbackComment}, NOW(), NOW())")
    @Options(useGeneratedKeys = true, keyProperty = "id", keyColumn = "id")
    int insert(AiChatMessageRecord record);

    @Select("SELECT * FROM ai_chat_message WHERE session_id = #{sessionId} ORDER BY id ASC")
    List<AiChatMessageRecord> selectBySessionId(@Param("sessionId") Long sessionId);

    @Select("SELECT * FROM ai_chat_message WHERE id = #{id} LIMIT 1")
    AiChatMessageRecord selectById(@Param("id") Long id);

    @Update("UPDATE ai_chat_message SET content = #{content}, citations_json = #{citationsJson}, confidence = #{confidence}, refuse_reason = #{refuseReason}, fallback = #{fallback}, updated_at = NOW() WHERE id = #{id}")
    int updateMessageContent(@Param("id") Long id,
                             @Param("content") String content,
                             @Param("citationsJson") String citationsJson,
                             @Param("confidence") Double confidence,
                             @Param("refuseReason") String refuseReason,
                             @Param("fallback") Boolean fallback);

    @Update("UPDATE ai_chat_message SET useful = #{useful}, feedback_comment = #{feedbackComment}, updated_at = NOW() WHERE id = #{id}")
    int updateFeedback(@Param("id") Long id, @Param("useful") Boolean useful, @Param("feedbackComment") String feedbackComment);

    @org.apache.ibatis.annotations.Delete("DELETE FROM ai_chat_message WHERE session_id = #{sessionId}")
    int deleteBySessionId(@Param("sessionId") Long sessionId);

    @org.apache.ibatis.annotations.Delete("DELETE FROM ai_chat_message WHERE session_id IN (SELECT id FROM ai_chat_session WHERE user_id = #{userId})")
    int deleteByUserId(@Param("userId") Long userId);
}
