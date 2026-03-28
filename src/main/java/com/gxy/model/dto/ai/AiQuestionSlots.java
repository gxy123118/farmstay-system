package com.gxy.model.dto.ai;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class AiQuestionSlots {

    private String city;

    private String travelGroup;

    private Integer budgetMin;

    private Integer budgetMax;

    private List<String> preferences = new ArrayList<>();

    private String topic;

    private String timeRange;
}
