package com.gxy.model.dto;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class FarmStayResourceSaveResponse {

    private FarmStayResponse farmStay;

    private List<RoomResponse> rooms = new ArrayList<>();

    private List<DiningResponse> dinings = new ArrayList<>();

    private List<ActivityResponse> activities = new ArrayList<>();
}

