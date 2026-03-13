package com.gxy.service;

import com.gxy.model.dto.ai.OperatorInsightAnalysisContext;
import com.gxy.model.dto.ai.OperatorInsightAiResult;

public interface OperatorInsightAiService {

    OperatorInsightAiResult analyze(OperatorInsightAnalysisContext context);
}
