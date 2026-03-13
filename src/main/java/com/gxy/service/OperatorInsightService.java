package com.gxy.service;

import com.gxy.model.dto.OperatorInsightIssueResponse;
import com.gxy.model.dto.OperatorInsightReportResponse;

import java.util.List;

public interface OperatorInsightService {

    OperatorInsightReportResponse generate(Long farmStayId, Integer periodDays);

    OperatorInsightReportResponse latest(Long farmStayId);

    List<OperatorInsightReportResponse> history(Long farmStayId);

    OperatorInsightReportResponse historyDetail(Long farmStayId, Long reportId);

    boolean deleteHistory(Long farmStayId, Long reportId);

    List<OperatorInsightIssueResponse> issues(Long farmStayId);
}
