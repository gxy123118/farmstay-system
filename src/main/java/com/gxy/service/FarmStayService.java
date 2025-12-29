package com.gxy.service;

import com.gxy.model.dto.FarmStayRequest;
import com.gxy.model.dto.FarmStayResponse;

import java.util.List;

public interface FarmStayService {

    /**
     * 获取所有公开的农家乐列表
     */
    List<FarmStayResponse> list(String city, String keyword, String priceLevel, String tag);

    /**
     * 根据编号查看农家乐详情
     */
    FarmStayResponse detail(Long id);

    /**
     * 经营者新建农家乐
     */
    FarmStayResponse create(FarmStayRequest request);

    /**
     * 经营者更新自家农家乐信息
     */
    FarmStayResponse update(Long id, FarmStayRequest request);
}
