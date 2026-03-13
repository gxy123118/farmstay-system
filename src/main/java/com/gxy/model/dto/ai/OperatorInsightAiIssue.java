package com.gxy.model.dto.ai;

import lombok.Data;

import java.util.List;

@Data
public class OperatorInsightAiIssue {

    private String topic;

    private String priority;

    private Integer issueCount;

    private Double negativeRatio;

    private Integer impactScore;

    private Integer evidenceCount;

    private List<String> evidenceSamples;
}
