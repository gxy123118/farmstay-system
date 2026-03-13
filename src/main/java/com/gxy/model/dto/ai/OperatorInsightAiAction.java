package com.gxy.model.dto.ai;

import lombok.Data;

@Data
public class OperatorInsightAiAction {

    private String title;

    private String topic;

    private String action;

    private String level;

    private String expectedBenefit;

    private String owner;

    private String reason;
}
