package com.gxy.service;

import com.gxy.model.dto.AiChatFeedbackRequest;
import com.gxy.model.dto.AiChatMessageRequest;
import com.gxy.model.dto.AiChatMessageResponse;
import com.gxy.model.dto.AiChatSessionCreateRequest;
import com.gxy.model.dto.AiChatSessionResponse;
import com.gxy.model.dto.AiChatSessionUpdateRequest;
import com.gxy.model.dto.AiChatStreamEventResponse;
import reactor.core.publisher.Flux;
import org.springframework.http.codec.ServerSentEvent;

import java.util.List;

public interface AiChatService {

    List<AiChatSessionResponse> listSessions();

    AiChatSessionResponse createSession(AiChatSessionCreateRequest request);

    AiChatSessionResponse getSession(Long sessionId);

    AiChatSessionResponse updateSession(Long sessionId, AiChatSessionUpdateRequest request);

    AiChatMessageResponse sendMessage(Long sessionId, AiChatMessageRequest request);

    Flux<ServerSentEvent<AiChatStreamEventResponse>> streamMessage(Long sessionId, AiChatMessageRequest request);

    List<AiChatMessageResponse> listMessages(Long sessionId);

    boolean feedback(AiChatFeedbackRequest request);

    void deleteSession(Long sessionId);

    void clearSessions();
}
