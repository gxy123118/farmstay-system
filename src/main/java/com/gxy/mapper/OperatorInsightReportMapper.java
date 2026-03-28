package com.gxy.mapper;

import com.gxy.model.entity.OperatorInsightReportRecord;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

@Mapper
public interface OperatorInsightReportMapper {

    @Insert("INSERT INTO operator_insight_report(report_id, farm_stay_id, owner_id, period_days, generation_mode, model, review_count, average_rating, summary, report_json, deleted, generated_at, created_at, updated_at) " +
            "VALUES(#{reportId}, #{farmStayId}, #{ownerId}, #{periodDays}, #{generationMode}, #{model}, #{reviewCount}, #{averageRating}, #{summary}, #{reportJson}, 0, #{generatedAt}, NOW(), NOW())")
    int insert(OperatorInsightReportRecord record);

    @Select("SELECT * FROM operator_insight_report WHERE farm_stay_id = #{farmStayId} AND deleted = 0 ORDER BY generated_at DESC, id DESC")
    List<OperatorInsightReportRecord> selectByFarmStayId(@Param("farmStayId") Long farmStayId);

    @Select("SELECT * FROM operator_insight_report WHERE farm_stay_id = #{farmStayId} AND report_id = #{reportId} AND deleted = 0 LIMIT 1")
    OperatorInsightReportRecord selectByReportId(@Param("farmStayId") Long farmStayId, @Param("reportId") Long reportId);

    @Select("SELECT * FROM operator_insight_report WHERE farm_stay_id = #{farmStayId} AND deleted = 0 ORDER BY generated_at DESC, id DESC LIMIT 1")
    OperatorInsightReportRecord selectLatestByFarmStayId(@Param("farmStayId") Long farmStayId);

    @Select("SELECT COALESCE(MAX(report_id), 5000) FROM operator_insight_report")
    Long selectMaxReportId();

    @Update("UPDATE operator_insight_report SET deleted = 1, updated_at = NOW() WHERE farm_stay_id = #{farmStayId} AND report_id = #{reportId} AND deleted = 0")
    int softDeleteByReportId(@Param("farmStayId") Long farmStayId, @Param("reportId") Long reportId);
}
