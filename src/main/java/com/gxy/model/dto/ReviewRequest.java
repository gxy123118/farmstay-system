package com.gxy.model.dto;

import lombok.Data;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

/**
 * 评价提交请求
 */
@Data
public class ReviewRequest {

    @NotNull(message = "订单ID不能为空")
    private Long orderId;

    @NotNull(message = "农家乐ID不能为空")
    private Long farmStayId;

    @Min(value = 1, message = "评分不能低于1分")
    @Max(value = 5, message = "评分不能超过5分")
    private Integer rating;

    @NotBlank(message = "评价内容不能为空")
    private String content;
}
