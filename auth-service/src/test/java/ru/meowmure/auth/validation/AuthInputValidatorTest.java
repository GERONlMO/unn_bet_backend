package ru.meowmure.auth.validation;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import static org.junit.jupiter.api.Assertions.*;

class AuthInputValidatorTest {

    @Test
    void acceptsValidUsername() {
        assertEquals("player_1", AuthInputValidator.validateAndNormalizeUsername("  player_1  "));
    }

    @Test
    void rejectsXssInUsername() {
        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> AuthInputValidator.validateAndNormalizeUsername("<script>alert(1)</script>"));
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
    }

    @Test
    void rejectsSqlLikeUsername() {
        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> AuthInputValidator.validateAndNormalizeUsername("admin' OR '1'='1"));
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
    }

    @Test
    void rejectsShortPasswordOnRegistration() {
        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> AuthInputValidator.validatePasswordForRegistration("abc"));
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
    }

    @Test
    void acceptsStrongPasswordOnRegistration() {
        assertDoesNotThrow(() -> AuthInputValidator.validatePasswordForRegistration("SecurePass1!"));
    }

    @Test
    void loginPasswordAllowsAnyNonEmptyLengthWithinLimit() {
        assertDoesNotThrow(() -> AuthInputValidator.validatePasswordForLogin("abc"));
    }

    @Test
    void validatesEmailFormat() {
        assertEquals("user@test.com", AuthInputValidator.validateAndNormalizeEmail(" User@Test.COM "));
    }

    @Test
    void rejectsXssInEmail() {
        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> AuthInputValidator.validateAndNormalizeEmail("x@y.com<script>"));
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
    }
}
