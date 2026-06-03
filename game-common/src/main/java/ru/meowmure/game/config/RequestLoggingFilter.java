package ru.meowmure.game.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class RequestLoggingFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(RequestLoggingFilter.class);

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        long start = System.currentTimeMillis();
        String user = request.getHeader("loggedInUser");
        String method = request.getMethod();
        String uri = request.getRequestURI();
        String query = request.getQueryString();
        String target = query != null ? uri + "?" + query : uri;
        String clientIp = request.getRemoteAddr();

        log.info("[HTTP IN]  {} {} | user={} | ip={}", method, target, formatUser(user), clientIp);

        try {
            filterChain.doFilter(request, response);
        } finally {
            long durationMs = System.currentTimeMillis() - start;
            int status = response.getStatus();
            if (status >= 400) {
                log.warn("[HTTP OUT] {} {} | user={} | status={} | {}ms", method, target, formatUser(user), status, durationMs);
            } else {
                log.info("[HTTP OUT] {} {} | user={} | status={} | {}ms", method, target, formatUser(user), status, durationMs);
            }
        }
    }

    private static String formatUser(String user) {
        return user != null && !user.isEmpty() ? user : "-";
    }
}
