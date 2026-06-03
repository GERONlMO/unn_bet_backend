package ru.meowmure.game.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import ru.meowmure.game.dto.PlayerProfileSummary;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class ProfileClient {

    private static final Logger log = LoggerFactory.getLogger(ProfileClient.class);

    private final RestTemplate restTemplate;

    @Value("${auth.service.url:http://auth-service:8081}")
    private String authServiceUrl;

    @Value("${internal.service.token:dev-internal-token}")
    private String internalToken;

    public ProfileClient(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    public Map<String, PlayerProfileSummary> getProfiles(Collection<String> usernames) {
        if (usernames == null || usernames.isEmpty()) {
            return Map.of();
        }
        List<String> unique = usernames.stream()
                .filter(u -> u != null && !u.isBlank())
                .distinct()
                .collect(Collectors.toList());
        if (unique.isEmpty()) {
            return Map.of();
        }
        try {
            HttpEntity<List<String>> entity = new HttpEntity<>(unique, internalHeaders());
            ResponseEntity<Map<String, PlayerProfileSummary>> response = restTemplate.exchange(
                    authServiceUrl + "/internal/profile/batch",
                    HttpMethod.POST,
                    entity,
                    new ParameterizedTypeReference<>() {}
            );
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                return response.getBody();
            }
        } catch (Exception e) {
            log.warn("[PROFILE] batch fetch FAILED users={} | error={}", unique, e.getMessage());
        }
        return new HashMap<>();
    }

    private HttpHeaders internalHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.set("X-Internal-Token", internalToken);
        return headers;
    }
}
