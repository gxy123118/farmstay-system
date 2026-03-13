package com.gxy.model.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OperatorInsightActionResponse {

    private String title;

    private String topic;

    private String action;

    private String level;

    private String expectedBenefit;

    private String owner;

    private String reason;
}
