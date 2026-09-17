package com.pidms.pidmsbackend.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor

public class PageInfo<T> {
    private int page;
    private int pageSize;
    private long total;
    private int pages;
    private List<T> list;
}
