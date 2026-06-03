package ru.meowmure.game.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;

@Service
public class WalletClient {

    private static final Logger log = LoggerFactory.getLogger(WalletClient.class);

    private final RestTemplate restTemplate;

    @Value("${wallet.service.url:http://wallet-service:8082}")
    private String walletServiceUrl;

    @Value("${internal.service.token:dev-internal-token}")
    private String internalToken;

    public WalletClient(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    public Long getBalance(String username) {
        try {
            ResponseEntity<Map> response = restTemplate.getForEntity(
                    walletServiceUrl + "/api/wallet/" + username, Map.class);
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                Object balanceObj = response.getBody().get("balance");
                if (balanceObj instanceof Number) {
                    return ((Number) balanceObj).longValue();
                }
            }
            return 0L;
        } catch (Exception e) {
            log.warn("[WALLET] getBalance FAILED user={} | error={}", username, e.getMessage());
            return 0L;
        }
    }

    public void deductBalance(String username, Long amount) {
        if (amount <= 0) {
            return;
        }
        log.info("[WALLET] deduct user={} amount={}", username, amount);
        try {
            HttpEntity<Void> entity = new HttpEntity<>(internalHeaders());
            restTemplate.exchange(
                    walletServiceUrl + "/internal/wallet/" + username + "/deduct?amount=" + amount,
                    HttpMethod.POST,
                    entity,
                    Void.class);
        } catch (Exception e) {
            log.warn("[WALLET] deduct FAILED user={} amount={} | error={}", username, amount, e.getMessage());
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Insufficient funds or wallet error for user: " + username);
        }
    }

    private HttpHeaders internalHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.set("X-Internal-Token", internalToken);
        return headers;
    }
}
