package com.gxy.service;

import com.gxy.model.dto.AiChatFeedbackRequest;
import com.gxy.model.dto.AiChatMessageRequest;
import com.gxy.model.dto.AiChatMessageResponse;
import com.gxy.model.dto.AiChatSessionCreateRequest;
import com.gxy.model.dto.AiChatSessionResponse;

import java.util.List;

public interface AiChatService {

    AiChatSessionResponse createSession(AiChatSessionCreateRequest request);

    AiChatSessionResponse getSession(Long sessionId);

    AiChatMessageResponse sendMessage(Long sessionId, AiChatMessageRequest request);

    List<AiChatMessageResponse> listMessages(Long sessionId);

    boolean feedback(AiChatFeedbackRequest request);
}

