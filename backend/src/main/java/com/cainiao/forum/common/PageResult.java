package com.cainiao.forum.common;

import java.util.Collections;
import java.util.List;

/**
 * 统一分页返回结构：records / total / page / size / pages。
 * 前台帖子列表、搜索、收藏列表和后台三张管理表共用同一结构，前端分页组件可直接复用。
 */
public class PageResult<T> {

    // 当前页数据。
    private List<T> records;

    // 满足条件的总条数。
    private long total;

    // 当前页码，从 1 开始。
    private int page;

    // 每页数量。
    private int size;

    // 总页数。
    private int pages;

    public PageResult() {
    }

    public PageResult(List<T> records, long total, int page, int size) {
        this.records = records;
        this.total = total;
        this.page = page;
        this.size = size;
        this.pages = total == 0 ? 1 : (int) Math.ceil(total * 1.0 / size);
    }

    public static <T> PageResult<T> of(List<T> records, long total, int page, int size) {
        if (records == null) {
            records = Collections.emptyList();
        }
        return new PageResult<>(records, total, page, size);
    }

    public List<T> getRecords() {
        return records;
    }

    public void setRecords(List<T> records) {
        this.records = records;
    }

    public long getTotal() {
        return total;
    }

    public void setTotal(long total) {
        this.total = total;
    }

    public int getPage() {
        return page;
    }

    public void setPage(int page) {
        this.page = page;
    }

    public int getSize() {
        return size;
    }

    public void setSize(int size) {
        this.size = size;
    }

    public int getPages() {
        return pages;
    }

    public void setPages(int pages) {
        this.pages = pages;
    }
}
