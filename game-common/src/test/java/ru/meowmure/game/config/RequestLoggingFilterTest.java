package ru.meowmure.game.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class RequestLoggingFilterTest {

    private RequestLoggingFilter filter;
    private MockHttpServletRequest request;
    private MockHttpServletResponse response;
    private FilterChain chain;

    @BeforeEach
    void setUp() {
        filter = new RequestLoggingFilter();
        request = new MockHttpServletRequest("GET", "/api/game/room/r1");
        response = new MockHttpServletResponse();
        chain = mock(FilterChain.class);
    }

    @Test
    void filterPassesThroughSuccessfully() throws ServletException, IOException {
        request.addHeader("loggedInUser", "alice");

        filter.doFilterInternal(request, response, chain);

        verify(chain).doFilter(request, response);
    }

    @Test
    void filterRunsWithQueryString() throws ServletException, IOException {
        request = new MockHttpServletRequest("POST", "/api/game/fold");
        request.setQueryString("roomId=r1");

        filter.doFilterInternal(request, response, chain);

        verify(chain).doFilter(request, response);
    }

    @Test
    void filterLogsWarningOnErrorStatus() throws ServletException, IOException {
        request.addHeader("loggedInUser", "alice");
        response.setStatus(400);

        filter.doFilterInternal(request, response, chain);

        verify(chain).doFilter(request, response);
    }
}
