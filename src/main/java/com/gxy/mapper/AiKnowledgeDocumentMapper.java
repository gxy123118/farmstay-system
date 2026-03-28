package com.gxy.mapper;

import com.gxy.model.entity.AiKnowledgeDocument;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

@Mapper
public interface AiKnowledgeDocumentMapper {

    @Select("<script>" +
            "SELECT * FROM ai_knowledge_document " +
            "WHERE status = 'ACTIVE' " +
            "AND (scope = 'public' OR (scope = 'operator_only' AND #{allowOperatorOnly} = 1)) " +
            "AND (farm_stay_id IS NULL OR farm_stay_id = #{farmStayId}) " +
            "<if test='keyword != null and keyword != \"\"'>" +
            "AND (title LIKE CONCAT('%', #{keyword}, '%') " +
            "OR content LIKE CONCAT('%', #{keyword}, '%') " +
            "OR keywords LIKE CONCAT('%', #{keyword}, '%')) " +
            "</if>" +
            "ORDER BY updated_at DESC " +
            "LIMIT #{limit}" +
            "</script>")
    List<AiKnowledgeDocument> search(@Param("keyword") String keyword,
                                     @Param("farmStayId") Long farmStayId,
                                     @Param("allowOperatorOnly") int allowOperatorOnly,
                                     @Param("limit") int limit);

    @Select("SELECT * FROM ai_knowledge_document WHERE status = 'ACTIVE' AND knowledge_code = #{knowledgeCode} LIMIT 1")
    AiKnowledgeDocument selectByKnowledgeCode(@Param("knowledgeCode") String knowledgeCode);

    @Select("SELECT * FROM ai_knowledge_document WHERE knowledge_code = #{knowledgeCode} LIMIT 1")
    AiKnowledgeDocument selectAnyByKnowledgeCode(@Param("knowledgeCode") String knowledgeCode);

    @Select("<script>" +
            "SELECT * FROM ai_knowledge_document " +
            "WHERE 1 = 1 " +
            "<if test='keyword != null and keyword != \"\"'>" +
            "AND (knowledge_code LIKE CONCAT('%', #{keyword}, '%') " +
            "OR title LIKE CONCAT('%', #{keyword}, '%') " +
            "OR content LIKE CONCAT('%', #{keyword}, '%') " +
            "OR keywords LIKE CONCAT('%', #{keyword}, '%')) " +
            "</if>" +
            "<if test='scope != null and scope != \"\"'>" +
            "AND scope = #{scope} " +
            "</if>" +
            "<if test='status != null and status != \"\"'>" +
            "AND status = #{status} " +
            "</if>" +
            "<if test='farmStayId != null'>" +
            "AND farm_stay_id = #{farmStayId} " +
            "</if>" +
            "<if test='farmStayId == null and includePlatformOnly != null and includePlatformOnly'>" +
            "AND farm_stay_id IS NULL " +
            "</if>" +
            "ORDER BY updated_at DESC " +
            "LIMIT #{offset}, #{pageSize}" +
            "</script>")
    List<AiKnowledgeDocument> selectPage(@Param("keyword") String keyword,
                                         @Param("scope") String scope,
                                         @Param("status") String status,
                                         @Param("farmStayId") Long farmStayId,
                                         @Param("includePlatformOnly") Boolean includePlatformOnly,
                                         @Param("offset") int offset,
                                         @Param("pageSize") int pageSize);

    @Select("<script>" +
            "SELECT COUNT(*) FROM ai_knowledge_document " +
            "WHERE 1 = 1 " +
            "<if test='keyword != null and keyword != \"\"'>" +
            "AND (knowledge_code LIKE CONCAT('%', #{keyword}, '%') " +
            "OR title LIKE CONCAT('%', #{keyword}, '%') " +
            "OR content LIKE CONCAT('%', #{keyword}, '%') " +
            "OR keywords LIKE CONCAT('%', #{keyword}, '%')) " +
            "</if>" +
            "<if test='scope != null and scope != \"\"'>" +
            "AND scope = #{scope} " +
            "</if>" +
            "<if test='status != null and status != \"\"'>" +
            "AND status = #{status} " +
            "</if>" +
            "<if test='farmStayId != null'>" +
            "AND farm_stay_id = #{farmStayId} " +
            "</if>" +
            "<if test='farmStayId == null and includePlatformOnly != null and includePlatformOnly'>" +
            "AND farm_stay_id IS NULL " +
            "</if>" +
            "</script>")
    long countPage(@Param("keyword") String keyword,
                   @Param("scope") String scope,
                   @Param("status") String status,
                   @Param("farmStayId") Long farmStayId,
                   @Param("includePlatformOnly") Boolean includePlatformOnly);

    @Select("SELECT * FROM ai_knowledge_document WHERE id = #{id} LIMIT 1")
    AiKnowledgeDocument selectById(@Param("id") Long id);

    @Select("SELECT COUNT(*) FROM ai_knowledge_document WHERE knowledge_code = #{knowledgeCode} AND id != #{excludeId}")
    long countByKnowledgeCodeExcludingId(@Param("knowledgeCode") String knowledgeCode, @Param("excludeId") Long excludeId);

    @Insert("INSERT INTO ai_knowledge_document(knowledge_code, title, content, summary, keywords, scope, farm_stay_id, status, created_by, updated_by, created_at, updated_at) " +
            "VALUES(#{knowledgeCode}, #{title}, #{content}, #{summary}, #{keywords}, #{scope}, #{farmStayId}, #{status}, #{createdBy}, #{updatedBy}, NOW(), NOW())")
    @Options(useGeneratedKeys = true, keyProperty = "id", keyColumn = "id")
    int insert(AiKnowledgeDocument document);

    @Update("UPDATE ai_knowledge_document SET " +
            "knowledge_code = #{knowledgeCode}, title = #{title}, content = #{content}, summary = #{summary}, keywords = #{keywords}, " +
            "scope = #{scope}, farm_stay_id = #{farmStayId}, status = #{status}, " +
            "updated_by = #{updatedBy}, updated_at = NOW() " +
            "WHERE id = #{id}")
    int updateById(AiKnowledgeDocument document);

    @Update("UPDATE ai_knowledge_document SET status = #{status}, updated_by = #{updatedBy}, updated_at = NOW() WHERE id = #{id}")
    int updateStatus(@Param("id") Long id, @Param("status") String status, @Param("updatedBy") Long updatedBy);

    @Delete("DELETE FROM ai_knowledge_document WHERE id = #{id}")
    int deleteById(@Param("id") Long id);
}
