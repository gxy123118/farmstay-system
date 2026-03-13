package com.gxy.model.dto.ai;

import lombok.Data;

import java.util.List;

@Data
public class OperatorInsightAiResult {

    private String summary;

    private List<OperatorInsightAiIssue> issues;

    private List<OperatorInsightAiAction> actions;
}
