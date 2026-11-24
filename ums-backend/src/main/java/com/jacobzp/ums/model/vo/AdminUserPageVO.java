package com.jacobzp.ums.model.vo;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class AdminUserPageVO {
    private List<AdminUserVO> items;
    private long total;
    private int page;
    private int size;
}
