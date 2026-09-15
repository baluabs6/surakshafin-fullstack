package com.surakshafin.config;

import org.springframework.security.core.context.SecurityContextHolder;

/** Small helper so controllers in any module can read "who is calling" without repeating boilerplate. */
public final class CurrentUser {
    private CurrentUser() {}

    public static Long id() {
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        return (Long) principal;
    }
}
