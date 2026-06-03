package ru.meowmure.auth.validation;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.regex.Pattern;

public final class AuthInputValidator {

    private static final Pattern USERNAME_PATTERN = Pattern.compile("^[\\p{L}\\p{N}_\\-]{3,24}$");
    private static final Pattern EMAIL_PATTERN = Pattern.compile(
            "^[\\p{L}\\p{N}._%+\\-]+@[\\p{L}\\p{N}.\\-]+\\.[\\p{L}]{2,63}$"
    );
    private static final Pattern DANGEROUS_MARKUP = Pattern.compile(
            "(?i)<[^>]*>|javascript:|vbscript:|data:text/html|on\\w+\\s*="
    );

    private AuthInputValidator() {
    }

    public static String validateAndNormalizeUsername(String username) {
        if (username == null) {
            throw badRequest("Username cannot be empty");
        }
        String normalized = username.trim();
        if (normalized.isEmpty()) {
            throw badRequest("Username cannot be empty");
        }
        if (containsDangerousContent(normalized)) {
            throw badRequest("Username contains invalid characters");
        }
        if (!USERNAME_PATTERN.matcher(normalized).matches()) {
            throw badRequest("Username must be 3-24 characters: letters, numbers, _ or -");
        }
        return normalized;
    }

    public static void validatePasswordForRegistration(String password) {
        if (password == null || password.isBlank()) {
            throw badRequest("Password cannot be empty");
        }
        if (password.length() < 6 || password.length() > 128) {
            throw badRequest("Password must be 6-128 characters");
        }
        if (containsControlCharacters(password)) {
            throw badRequest("Password contains invalid characters");
        }
    }

    public static void validatePasswordForLogin(String password) {
        if (password == null || password.isEmpty()) {
            throw badRequest("Password cannot be empty");
        }
        if (password.length() > 128) {
            throw badRequest("Password is too long");
        }
    }

    public static String validateAndNormalizeEmail(String email) {
        if (email == null || email.isBlank()) {
            return null;
        }
        String normalized = email.trim();
        if (containsDangerousContent(normalized)) {
            throw badRequest("Email contains invalid characters");
        }
        if (normalized.length() > 254 || !EMAIL_PATTERN.matcher(normalized).matches()) {
            throw badRequest("Invalid email format");
        }
        return normalized.toLowerCase();
    }

    static boolean containsDangerousContent(String value) {
        return value.indexOf('<') >= 0
                || value.indexOf('>') >= 0
                || DANGEROUS_MARKUP.matcher(value).find();
    }

    static boolean containsControlCharacters(String value) {
        return value.chars().anyMatch(c -> c < 32 || c == 127);
    }

    private static ResponseStatusException badRequest(String message) {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
    }
}
