package com.surakshafin.config;

import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/** Small helper so controllers in any module can read "who is calling" without repeating boilerplate. */
public final class CurrentUser {
    private CurrentUser() {}

    public static Long id() {
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        return (Long) principal;
    }

    public static boolean isAdmin() {
        return SecurityContextHolder.getContext().getAuthentication().getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
    }

    /** The raw bearer token for this request, stashed by JwtAuthFilter — used by /auth/logout. */
    public static String rawToken() {
        ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attrs == null) {
            return null;
        }
        Object token = attrs.getRequest().getAttribute("surakshafin.rawToken");
        return token == null ? null : token.toString();
    }
}
