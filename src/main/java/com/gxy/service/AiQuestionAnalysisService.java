package com.gxy.service;

import com.gxy.model.dto.ai.AiQuestionAnalysisResult;

import java.util.List;

public interface AiQuestionAnalysisService {

    AiQuestionAnalysisResult analyze(Long farmStayId, String scene, String question, List<String> recentMessages);
}
