package com.gxy.service;

import com.gxy.model.dto.AiCitationResponse;
import com.gxy.model.dto.ai.AiQuestionAnalysisResult;

import java.util.List;

public interface AiPromptBuilder {

    String buildChatPrompt(Long farmStayId,
                           String scene,
                           String question,
                           List<String> recentMessages,
                           List<AiCitationResponse> citations,
                           AiQuestionAnalysisResult analysis);
}
