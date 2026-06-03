package ru.meowmure.game.config;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.server.ResponseStatusException;

import static org.junit.jupiter.api.Assertions.*;

class GlobalExceptionLoggerTest {

    private final GlobalExceptionLogger logger = new GlobalExceptionLogger();

    @Test
    void handlesResponseStatusException() {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/game/fold");
        request.addHeader("loggedInUser", "alice");
        ResponseStatusException ex = new ResponseStatusException(HttpStatus.BAD_REQUEST, "Not your turn");

        var response = logger.handleResponseStatusException(ex, request);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }
}
