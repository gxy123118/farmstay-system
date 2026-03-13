package com.gxy.model.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AiCitationResponse {

    private String sourceType;

    private String sourceId;

    private String snippet;
}

