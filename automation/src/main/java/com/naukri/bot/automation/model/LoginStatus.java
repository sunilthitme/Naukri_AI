package com.naukri.bot.automation.model;

public enum LoginStatus {
    SUCCESS,
    INVALID_CREDENTIALS,
    CAPTCHA_DETECTED,
    OTP_REQUIRED,
    LOGIN_FORM_NOT_FOUND,
    STILL_ON_LOGIN_PAGE,
    FAILED
}
