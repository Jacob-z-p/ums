package com.jacobzp.ums.service;

public class AccountAlreadyExistsException extends RuntimeException {
    public AccountAlreadyExistsException(Throwable cause) {
        super("账号已存在", cause);
    }
}
