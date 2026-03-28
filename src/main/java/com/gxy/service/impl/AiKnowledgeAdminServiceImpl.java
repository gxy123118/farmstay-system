package com.gxy.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import com.gxy.common.PageResponse;
import com.gxy.common.auth.AuthGuard;
import com.gxy.common.exception.BusinessException;
import com.gxy.mapper.AiKnowledgeDocumentMapper;
import com.gxy.mapper.FarmStayMapper;
import com.gxy.model.dto.AiCitationResponse;
import com.gxy.model.dto.AiKnowledgeDocumentRequest;
import com.gxy.model.dto.AiKnowledgeDocumentResponse;
import com.gxy.model.entity.AiKnowledgeDocument;
import com.gxy.model.entity.FarmStay;
import com.gxy.service.AiKnowledgeAdminService;
import com.gxy.service.AiKnowledgeRetriever;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AiKnowledgeAdminServiceImpl implements AiKnowledgeAdminService {

    private static final String STATUS_ACTIVE = "ACTIVE";
    private static final String STATUS_INACTIVE = "INACTIVE";
    private static final String SCOPE_PUBLIC = "public";
    private static final String SCOPE_OPERATOR_ONLY = "operator_only";

    private final AiKnowledgeDocumentMapper aiKnowledgeDocumentMapper;
    private final FarmStayMapper farmStayMapper;
    private final AiKnowledgeRetriever aiKnowledgeRetriever;

    @Override
    public PageResponse<AiKnowledgeDocumentResponse> list(String keyword,
                                                          String scope,
                                                          String status,
                                                          Long farmStayId,
                                                          Boolean platformOnly,
                                                          Integer page,
                                                          Integer pageSize) {
        AuthGuard.enforceAtLeastOperator();
        if (farmStayId != null && !AuthGuard.isAdmin()) {
            validateFarmStayOwnership(farmStayId);
        }

        int currentPage = page == null || page < 1 ? 1 : page;
        int currentPageSize = pageSize == null || pageSize < 1 ? 10 : Math.min(pageSize, 100);
        int offset = (currentPage - 1) * currentPageSize;

        List<AiKnowledgeDocument> documents = aiKnowledgeDocumentMapper.selectPage(
                trim(keyword),
                trim(scope),
                trim(status),
                farmStayId,
                platformOnly,
                offset,
                currentPageSize
        );
        long total = aiKnowledgeDocumentMapper.countPage(
                trim(keyword),
                trim(scope),
                trim(status),
                farmStayId,
                platformOnly
        );

        List<AiKnowledgeDocumentResponse> responses = new ArrayList<>();
        for (AiKnowledgeDocument document : documents) {
            responses.add(toResponse(document));
        }
        return PageResponse.of(responses, total, currentPage, currentPageSize);
    }

    @Override
    public AiKnowledgeDocumentResponse detail(Long id) {
        AuthGuard.enforceAtLeastOperator();
        return toResponse(requireAccessibleDocument(id));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AiKnowledgeDocumentResponse create(AiKnowledgeDocumentRequest request) {
        AuthGuard.enforceAtLeastOperator();
        validateRequest(request);
        long duplicate = aiKnowledgeDocumentMapper.countByKnowledgeCodeExcludingId(
                request.getKnowledgeCode().trim(),
                -1L
        );
        if (duplicate > 0) {
            throw new BusinessException("knowledgeCode已存在");
        }
        if (request.getFarmStayId() != null && !AuthGuard.isAdmin()) {
            validateFarmStayOwnership(request.getFarmStayId());
        }

        Long userId = StpUtil.getLoginIdAsLong();
        AiKnowledgeDocument document = new AiKnowledgeDocument();
        fillDocument(document, request, userId);
        document.setCreatedBy(userId);
        if (aiKnowledgeDocumentMapper.insert(document) <= 0) {
            throw new BusinessException("知识片段创建失败");
        }
        return toResponse(requireDocument(document.getId()));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AiKnowledgeDocumentResponse update(Long id, AiKnowledgeDocumentRequest request) {
        AuthGuard.enforceAtLeastOperator();
        validateRequest(request);
        AiKnowledgeDocument existing = requireAccessibleDocument(id);
        if (request.getFarmStayId() != null && !AuthGuard.isAdmin()) {
            validateFarmStayOwnership(request.getFarmStayId());
        }

        long duplicate = aiKnowledgeDocumentMapper.countByKnowledgeCodeExcludingId(
                request.getKnowledgeCode().trim(),
                id
        );
        if (duplicate > 0) {
            throw new BusinessException("knowledgeCode已存在");
        }

        Long userId = StpUtil.getLoginIdAsLong();
        existing.setId(id);
        fillDocument(existing, request, userId);
        if (aiKnowledgeDocumentMapper.updateById(existing) <= 0) {
            throw new BusinessException("知识片段更新失败");
        }
        return toResponse(requireDocument(id));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public List<AiKnowledgeDocumentResponse> batchUpsert(List<AiKnowledgeDocumentRequest> requests) {
        AuthGuard.enforceAtLeastOperator();
        if (requests == null || requests.isEmpty()) {
            return Collections.emptyList();
        }

        List<AiKnowledgeDocumentResponse> responses = new ArrayList<>();
        Long userId = StpUtil.getLoginIdAsLong();
        for (AiKnowledgeDocumentRequest request : requests) {
            validateRequest(request);
            if (request.getFarmStayId() != null && !AuthGuard.isAdmin()) {
                validateFarmStayOwnership(request.getFarmStayId());
            }

            AiKnowledgeDocument existing =
                    aiKnowledgeDocumentMapper.selectAnyByKnowledgeCode(request.getKnowledgeCode().trim());
            if (existing == null) {
                AiKnowledgeDocument document = new AiKnowledgeDocument();
                fillDocument(document, request, userId);
                document.setCreatedBy(userId);
                if (aiKnowledgeDocumentMapper.insert(document) <= 0) {
                    throw new BusinessException("批量导入失败");
                }
                responses.add(toResponse(requireDocument(document.getId())));
                continue;
            }

            ensureDocumentAccessible(existing);
            fillDocument(existing, request, userId);
            if (aiKnowledgeDocumentMapper.updateById(existing) <= 0) {
                throw new BusinessException("批量更新失败");
            }
            responses.add(toResponse(requireDocument(existing.getId())));
        }
        return responses;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AiKnowledgeDocumentResponse updateStatus(Long id, String status) {
        AuthGuard.enforceAtLeastOperator();
        requireAccessibleDocument(id);
        String normalizedStatus = normalizeStatus(status);
        if (!STATUS_ACTIVE.equals(normalizedStatus) && !STATUS_INACTIVE.equals(normalizedStatus)) {
            throw new BusinessException("status仅支持ACTIVE或INACTIVE");
        }

        Long userId = StpUtil.getLoginIdAsLong();
        if (aiKnowledgeDocumentMapper.updateStatus(id, normalizedStatus, userId) <= 0) {
            throw new BusinessException("知识片段状态更新失败");
        }
        return toResponse(requireDocument(id));
    }

    @Override
    public List<AiCitationResponse> previewRetrieve(Long farmStayId, String scene, String question) {
        AuthGuard.enforceAtLeastOperator();
        if (farmStayId != null && !AuthGuard.isAdmin()) {
            validateFarmStayOwnership(farmStayId);
        }
        return aiKnowledgeRetriever.retrieve(farmStayId, scene, question, true);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        AuthGuard.enforceAtLeastOperator();
        AiKnowledgeDocument document = requireAccessibleDocument(id);
        if (aiKnowledgeDocumentMapper.deleteById(document.getId()) <= 0) {
            throw new BusinessException("知识片段删除失败");
        }
    }

    private AiKnowledgeDocument requireAccessibleDocument(Long id) {
        AiKnowledgeDocument document = requireDocument(id);
        ensureDocumentAccessible(document);
        return document;
    }

    private void ensureDocumentAccessible(AiKnowledgeDocument document) {
        if (AuthGuard.isAdmin()) {
            return;
        }
        if (document.getFarmStayId() != null) {
            validateFarmStayOwnership(document.getFarmStayId());
        }
    }

    private AiKnowledgeDocument requireDocument(Long id) {
        AiKnowledgeDocument document = aiKnowledgeDocumentMapper.selectById(id);
        if (document == null) {
            throw new BusinessException("知识片段不存在");
        }
        return document;
    }

    private void validateRequest(AiKnowledgeDocumentRequest request) {
        if (request == null) {
            throw new BusinessException("请求不能为空");
        }
        String scope = trim(request.getScope());
        if (!SCOPE_PUBLIC.equals(scope) && !SCOPE_OPERATOR_ONLY.equals(scope)) {
            throw new BusinessException("scope仅支持public或operator_only");
        }
        String status = normalizeStatus(request.getStatus());
        if (!STATUS_ACTIVE.equals(status) && !STATUS_INACTIVE.equals(status)) {
            throw new BusinessException("status仅支持ACTIVE或INACTIVE");
        }
    }

    private void validateFarmStayOwnership(Long farmStayId) {
        Long ownerId = StpUtil.getLoginIdAsLong();
        FarmStay farmStay = farmStayMapper.selectByIdAndOwner(farmStayId, ownerId);
        if (farmStay == null) {
            throw new BusinessException("无权操作该民宿知识片段");
        }
    }

    private void fillDocument(AiKnowledgeDocument document, AiKnowledgeDocumentRequest request, Long userId) {
        document.setKnowledgeCode(request.getKnowledgeCode().trim());
        document.setTitle(request.getTitle().trim());
        document.setContent(request.getContent().trim());
        document.setSummary(trimToNull(request.getSummary()));
        document.setKeywords(trimToNull(request.getKeywords()));
        document.setScope(request.getScope().trim());
        document.setFarmStayId(request.getFarmStayId());
        document.setStatus(normalizeStatus(request.getStatus()));
        document.setUpdatedBy(userId);
    }

    private AiKnowledgeDocumentResponse toResponse(AiKnowledgeDocument document) {
        AiKnowledgeDocumentResponse response = new AiKnowledgeDocumentResponse();
        response.setId(document.getId());
        response.setKnowledgeCode(document.getKnowledgeCode());
        response.setTitle(document.getTitle());
        response.setContent(document.getContent());
        response.setSummary(document.getSummary());
        response.setKeywords(document.getKeywords());
        response.setScope(document.getScope());
        response.setFarmStayId(document.getFarmStayId());
        response.setStatus(document.getStatus());
        response.setCreatedBy(document.getCreatedBy());
        response.setUpdatedBy(document.getUpdatedBy());
        response.setCreatedAt(document.getCreatedAt());
        response.setUpdatedAt(document.getUpdatedAt());
        return response;
    }

    private String normalizeStatus(String status) {
        if (!StringUtils.hasText(status)) {
            return STATUS_ACTIVE;
        }
        return status.trim().toUpperCase();
    }

    private String trim(String text) {
        return StringUtils.hasText(text) ? text.trim() : null;
    }

    private String trimToNull(String text) {
        return StringUtils.hasText(text) ? text.trim() : null;
    }
}
