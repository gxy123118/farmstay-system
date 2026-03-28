package com.gxy.service;

import com.gxy.model.dto.AiCitationResponse;

import java.util.List;

public interface AiKnowledgeRetriever {

    List<AiCitationResponse> retrieve(Long farmStayId, String scene, String question, boolean operator);
}
