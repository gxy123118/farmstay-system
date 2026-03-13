package com.gxy.controller;

import com.gxy.common.ApiResponse;
import com.gxy.model.dto.AiChatFeedbackRequest;
import com.gxy.model.dto.AiChatMessageRequest;
import com.gxy.model.dto.AiChatMessageResponse;
import com.gxy.model.dto.AiChatSessionCreateRequest;
import com.gxy.model.dto.AiChatSessionResponse;
import com.gxy.service.AiChatService;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import java.util.List;

@RestController
@RequestMapping("/api/ai/chat")
@Validated
@RequiredArgsConstructor
public class AiChatController {

    private final AiChatService aiChatService;

    @PostMapping("/sessions")
    public ApiResponse<AiChatSessionResponse> createSession(@RequestBody(required = false) AiChatSessionCreateRequest request) {
        if (request == null) {
            request = new AiChatSessionCreateRequest();
        }
        return ApiResponse.ok(aiChatService.createSession(request));
    }

    @GetMapping("/sessions/{sessionId}")
    public ApiResponse<AiChatSessionResponse> getSession(@PathVariable Long sessionId) {
        return ApiResponse.ok(aiChatService.getSession(sessionId));
    }

    @PostMapping("/sessions/{sessionId}/messages")
    public ApiResponse<AiChatMessageResponse> sendMessage(@PathVariable Long sessionId,
                                                          @Valid @RequestBody AiChatMessageRequest request) {
        return ApiResponse.ok(aiChatService.sendMessage(sessionId, request));
    }

    @GetMapping("/sessions/{sessionId}/messages")
    public ApiResponse<List<AiChatMessageResponse>> listMessages(@PathVariable Long sessionId) {
        return ApiResponse.ok(aiChatService.listMessages(sessionId));
    }

    @PostMapping("/feedback")
    public ApiResponse<Boolean> feedback(@Valid @RequestBody AiChatFeedbackRequest request) {
        return ApiResponse.ok(aiChatService.feedback(request));
    }
}
