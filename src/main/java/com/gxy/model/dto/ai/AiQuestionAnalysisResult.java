package com.gxy.model.dto.ai;

import lombok.Data;

@Data
public class AiQuestionAnalysisResult {

    private String intent;

    private String faqQuery;

    private Boolean needsClarification;

    private String clarificationQuestion;

    private AiQuestionSlots slots = new AiQuestionSlots();
}
