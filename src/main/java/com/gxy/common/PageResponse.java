package com.gxy.common;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Collections;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PageResponse<T> {

    private List<T> list;

    private long total;

    private int page;

    private int pageSize;

    public static <T> PageResponse<T> of(List<T> list, long total, int page, int pageSize) {
        return new PageResponse<>(list == null ? Collections.emptyList() : list, total, page, pageSize);
    }
}
