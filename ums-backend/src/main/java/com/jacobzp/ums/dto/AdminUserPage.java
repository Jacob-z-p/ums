package com.jacobzp.ums.dto;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class AdminUserPage {
    private List<AdminUserResponse> items;
    private long total;
    private int page;
    private int size;
}
