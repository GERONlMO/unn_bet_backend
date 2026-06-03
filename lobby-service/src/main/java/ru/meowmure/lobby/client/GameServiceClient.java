package ru.meowmure.lobby.client;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import ru.meowmure.game.model.GameRoom;

@Service
public class GameServiceClient {

    private static final Logger log = LoggerFactory.getLogger(GameServiceClient.class);

    private final RestTemplate restTemplate;

    @Value("${game.service.url:http://game-service:8083}")
    private String gameServiceUrl;

    @Value("${internal.service.token:dev-internal-token}")
    private String internalToken;

    public GameServiceClient(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    public void notifyRoom(String roomId) {
        try {
            restTemplate.exchange(
                    gameServiceUrl + "/internal/room/" + roomId + "/notify",
                    HttpMethod.POST,
                    new HttpEntity<>(internalHeaders()),
                    Void.class
            );
            log.debug("[GAME CLIENT] notify room={}", roomId);
        } catch (Exception e) {
            log.warn("[GAME CLIENT] notify failed room={} | {}", roomId, e.getMessage());
        }
    }

    public GameRoom startGame(String roomId) {
        ResponseEntity<GameRoom> response = restTemplate.exchange(
                gameServiceUrl + "/internal/game/" + roomId + "/start",
                HttpMethod.POST,
                new HttpEntity<>(internalHeaders()),
                GameRoom.class
        );
        log.info("[GAME CLIENT] startGame room={}", roomId);
        return response.getBody();
    }

    private HttpHeaders internalHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.set("X-Internal-Token", internalToken);
        return headers;
    }
}
