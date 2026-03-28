package com.gxy.controller;

import com.gxy.common.ApiResponse;
import com.gxy.common.PageResponse;
import com.gxy.model.dto.AiCitationResponse;
import com.gxy.model.dto.AiKnowledgeDocumentRequest;
import com.gxy.model.dto.AiKnowledgeDocumentResponse;
import com.gxy.model.dto.AiKnowledgeRetrievePreviewRequest;
import com.gxy.model.dto.AiKnowledgeStatusRequest;
import com.gxy.service.AiKnowledgeAdminService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/ai/knowledge")
@Validated
@RequiredArgsConstructor
public class AiKnowledgeAdminController {

    private final AiKnowledgeAdminService aiKnowledgeAdminService;

    @GetMapping
    public ApiResponse<PageResponse<AiKnowledgeDocumentResponse>> list(@RequestParam(required = false) String keyword,
                                                                       @RequestParam(required = false) String docType,
                                                                       @RequestParam(required = false) String scope,
                                                                       @RequestParam(required = false) String status,
                                                                       @RequestParam(required = false) Long farmStayId,
                                                                       @RequestParam(required = false) Boolean platformOnly,
                                                                       @RequestParam(defaultValue = "1") Integer page,
                                                                       @RequestParam(defaultValue = "10") Integer pageSize) {
        return ApiResponse.ok(aiKnowledgeAdminService.list(keyword, docType, scope, status, farmStayId, platformOnly, page, pageSize));
    }

    @GetMapping("/{id}")
    public ApiResponse<AiKnowledgeDocumentResponse> detail(@PathVariable Long id) {
        return ApiResponse.ok(aiKnowledgeAdminService.detail(id));
    }

    @PostMapping
    public ApiResponse<AiKnowledgeDocumentResponse> create(@Valid @RequestBody AiKnowledgeDocumentRequest request) {
        return ApiResponse.ok(aiKnowledgeAdminService.create(request));
    }

    @PostMapping("/batch-upsert")
    public ApiResponse<List<AiKnowledgeDocumentResponse>> batchUpsert(@Valid @RequestBody List<AiKnowledgeDocumentRequest> requests) {
        return ApiResponse.ok(aiKnowledgeAdminService.batchUpsert(requests));
    }

    @PutMapping("/{id}")
    public ApiResponse<AiKnowledgeDocumentResponse> update(@PathVariable Long id,
                                                           @Valid @RequestBody AiKnowledgeDocumentRequest request) {
        return ApiResponse.ok(aiKnowledgeAdminService.update(id, request));
    }

    @PutMapping("/{id}/status")
    public ApiResponse<AiKnowledgeDocumentResponse> updateStatus(@PathVariable Long id,
                                                                 @Valid @RequestBody AiKnowledgeStatusRequest request) {
        return ApiResponse.ok(aiKnowledgeAdminService.updateStatus(id, request.getStatus()));
    }

    @PostMapping("/retrieve-preview")
    public ApiResponse<List<AiCitationResponse>> previewRetrieve(@Valid @RequestBody AiKnowledgeRetrievePreviewRequest request) {
        return ApiResponse.ok(aiKnowledgeAdminService.previewRetrieve(request.getFarmStayId(), request.getScene(), request.getQuestion()));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        aiKnowledgeAdminService.delete(id);
        return ApiResponse.ok();
    }
}
