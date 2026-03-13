package com.gxy.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "app.ai.operator-insight")
public class OperatorInsightAiProperties {

    private boolean enabled = true;

    private String model = "MiniMax-M2.5";

    private int maxReviewSamples = 20;
}
