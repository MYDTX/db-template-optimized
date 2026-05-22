package com.s365.dbtemplate;

import lombok.Data;

import java.util.List;

/**
 * 分页查询结果封装
 */
@Data
public class PageResult<T> {
    private long total;
    private int page;
    private int pageSize;
    private List<T> data;
    private int lastPage;

    public PageResult(long total, int page, int size, List<T> data) {
        this.total = total;
        this.page = page;
        this.pageSize = size;
        this.data = data;
        this.lastPage = size <= 0 ? 0 : (int) Math.ceil((double) total / (double) size);
    }
}
