package ru.meowmure.game.config;

import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

@RestControllerAdvice
public class GlobalExceptionLogger {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionLogger.class);

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<Void> handleResponseStatusException(ResponseStatusException ex, HttpServletRequest request) {
        String user = request.getHeader("loggedInUser");
        String query = request.getQueryString();
        String target = query != null ? request.getRequestURI() + "?" + query : request.getRequestURI();

        log.warn("[API REJECTED] {} {} | user={} | status={} | reason={}",
                request.getMethod(), target, user != null ? user : "-", ex.getStatusCode().value(), ex.getReason());

        return ResponseEntity.status(ex.getStatusCode()).build();
    }
}
