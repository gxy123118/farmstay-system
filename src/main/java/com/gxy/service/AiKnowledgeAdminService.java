package com.gxy.service;

import com.gxy.common.PageResponse;
import com.gxy.model.dto.AiCitationResponse;
import com.gxy.model.dto.AiKnowledgeDocumentRequest;
import com.gxy.model.dto.AiKnowledgeDocumentResponse;

import java.util.List;

public interface AiKnowledgeAdminService {

    PageResponse<AiKnowledgeDocumentResponse> list(String keyword,
                                                   String docType,
                                                   String scope,
                                                   String status,
                                                   Long farmStayId,
                                                   Boolean platformOnly,
                                                   Integer page,
                                                   Integer pageSize);

    AiKnowledgeDocumentResponse detail(Long id);

    AiKnowledgeDocumentResponse create(AiKnowledgeDocumentRequest request);

    AiKnowledgeDocumentResponse update(Long id, AiKnowledgeDocumentRequest request);

    List<AiKnowledgeDocumentResponse> batchUpsert(List<AiKnowledgeDocumentRequest> requests);

    AiKnowledgeDocumentResponse updateStatus(Long id, String status);

    List<AiCitationResponse> previewRetrieve(Long farmStayId, String scene, String question);

    void delete(Long id);
}
