package com.gxy.service;

import com.gxy.model.dto.ReviewRequest;
import com.gxy.model.dto.ReviewResponse;

import java.util.List;

public interface ReviewService {

    ReviewResponse createReview(ReviewRequest request);

    List<ReviewResponse> listForFarmStay(Long farmStayId);

    ReviewResponse moderate(Long reviewId, String status);
}
