package ru.meowmure.game.security;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

public final class UserRoles {

    public static final String USER = "USER";
    public static final String ADMIN = "ADMIN";

    private UserRoles() {
    }

    public static void requireAdmin(String role) {
        if (role == null || !ADMIN.equals(role)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Admin access required");
        }
    }

    public static void requireAuthenticated(String username) {
        if (username == null || username.isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Not authenticated");
        }
    }
}
