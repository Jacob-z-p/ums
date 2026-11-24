package com.jacobzp.ums.validation;

import java.util.regex.Pattern;

public final class CredentialsValidator {
    public static final String REQUIREMENTS = "账号须为 1–64 位英文字母或数字，密码至少需要 8 个字符";
    private static final Pattern ACCOUNT_PATTERN = Pattern.compile("[A-Za-z0-9]{1,64}");

    private CredentialsValidator() {
    }

    public static boolean isValidAccount(String account) {
        return account != null && ACCOUNT_PATTERN.matcher(account).matches();
    }

    public static boolean isValid(String account, String password) {
        return isValidAccount(account) && password != null && password.codePointCount(0, password.length()) >= 8;
    }
}
