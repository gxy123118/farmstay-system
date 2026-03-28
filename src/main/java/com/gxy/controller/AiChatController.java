package com.gxy.controller;

import com.gxy.common.ApiResponse;
import com.gxy.model.dto.AiChatFeedbackRequest;
import com.gxy.model.dto.AiChatMessageRequest;
import com.gxy.model.dto.AiChatMessageResponse;
import com.gxy.model.dto.AiChatSessionCreateRequest;
import com.gxy.model.dto.AiChatSessionResponse;
import com.gxy.model.dto.AiChatSessionUpdateRequest;
import com.gxy.model.dto.AiChatStreamEventResponse;
import com.gxy.service.AiChatService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.PutMapping;

import jakarta.validation.Valid;
import java.util.List;
import reactor.core.publisher.Flux;

@RestController
@RequestMapping("/api/ai/chat")
@Validated
@RequiredArgsConstructor
public class AiChatController {

    private final AiChatService aiChatService;

    @GetMapping("/sessions")
    public ApiResponse<List<AiChatSessionResponse>> listSessions() {
        return ApiResponse.ok(aiChatService.listSessions());
    }

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

    @PutMapping("/sessions/{sessionId}")
    public ApiResponse<AiChatSessionResponse> updateSession(@PathVariable Long sessionId,
                                                            @Valid @RequestBody AiChatSessionUpdateRequest request) {
        return ApiResponse.ok(aiChatService.updateSession(sessionId, request));
    }

    @PostMapping("/sessions/{sessionId}/messages")
    public ApiResponse<AiChatMessageResponse> sendMessage(@PathVariable Long sessionId,
                                                          @Valid @RequestBody AiChatMessageRequest request) {
        return ApiResponse.ok(aiChatService.sendMessage(sessionId, request));
    }

    @PostMapping(value = "/sessions/{sessionId}/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<AiChatStreamEventResponse>> streamMessage(@PathVariable Long sessionId,
                                                                          @Valid @RequestBody AiChatMessageRequest request) {
        return aiChatService.streamMessage(sessionId, request);
    }

    @GetMapping("/sessions/{sessionId}/messages")
    public ApiResponse<List<AiChatMessageResponse>> listMessages(@PathVariable Long sessionId) {
        return ApiResponse.ok(aiChatService.listMessages(sessionId));
    }

    @PostMapping("/feedback")
    public ApiResponse<Boolean> feedback(@Valid @RequestBody AiChatFeedbackRequest request) {
        return ApiResponse.ok(aiChatService.feedback(request));
    }

    @DeleteMapping("/sessions/{sessionId}")
    public ApiResponse<Void> deleteSession(@PathVariable Long sessionId) {
        aiChatService.deleteSession(sessionId);
        return ApiResponse.ok();
    }

    @DeleteMapping("/sessions")
    public ApiResponse<Void> clearSessions() {
        aiChatService.clearSessions();
        return ApiResponse.ok();
    }
}
