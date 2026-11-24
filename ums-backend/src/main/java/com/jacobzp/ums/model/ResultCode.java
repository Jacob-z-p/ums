package com.jacobzp.ums.model;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ResultCode {
    SUCCESS(0, "操作成功"),
    INVALID_PARAMETER(1001, "参数无效"),
    INVALID_CREDENTIALS(2001, "账号或密码错误"),
    ACCOUNT_EXISTS(2002, "账号已存在"),
    LOGIN_REQUIRED(2003, "请先登录"),
    ADMIN_REQUIRED(3001, "需要管理员权限"),
    ADMIN_PROTECTED(3002, "不能删除管理员账号"),
    USER_NOT_FOUND(3003, "用户不存在"),
    INTERNAL_ERROR(9000, "服务内部错误");

    private final int code;
    private final String message;
}
