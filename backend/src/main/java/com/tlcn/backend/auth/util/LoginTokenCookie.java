package com.tlcn.backend.auth.util;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Arrays;
import java.util.Optional;

public final class LoginTokenCookie {
    public static final String NAME = "TLCN_AUTH";

    private LoginTokenCookie() {}

    public static Optional<String> read(HttpServletRequest request) {
        if (request.getCookies() == null) return Optional.empty();
        return Arrays.stream(request.getCookies())
                .filter(cookie -> NAME.equals(cookie.getName()))
                .map(Cookie::getValue)
                .findFirst();
    }
}
