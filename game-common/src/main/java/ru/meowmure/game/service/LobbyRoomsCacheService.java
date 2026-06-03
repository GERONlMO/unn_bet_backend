package ru.meowmure.game.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Profile;
import org.springframework.context.event.EventListener;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import ru.meowmure.game.model.GameRoom;
import ru.meowmure.game.model.GameRoomRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

@Service
@Profile("!webmvc-test")
public class LobbyRoomsCacheService {

    public static final String CACHE_KEY = "lobby:rooms:snapshot";

    private static final Logger log = LoggerFactory.getLogger(LobbyRoomsCacheService.class);
    private static final TypeReference<List<GameRoom>> ROOM_LIST_TYPE = new TypeReference<>() {};

    private final StringRedisTemplate redisTemplate;
    private final GameRoomRepository gameRoomRepository;
    private final ObjectMapper objectMapper;

    public LobbyRoomsCacheService(StringRedisTemplate redisTemplate,
                                  GameRoomRepository gameRoomRepository,
                                  ObjectMapper objectMapper) {
        this.redisTemplate = redisTemplate;
        this.gameRoomRepository = gameRoomRepository;
        this.objectMapper = objectMapper;
    }

    public List<GameRoom> getAll() {
        String cached = redisTemplate.opsForValue().get(CACHE_KEY);
        if (cached != null && !cached.isBlank()) {
            try {
                List<GameRoom> rooms = objectMapper.readValue(cached, ROOM_LIST_TYPE);
                log.debug("[LOBBY CACHE] hit | totalRooms={}", rooms.size());
                return rooms;
            } catch (Exception e) {
                log.warn("[LOBBY CACHE] corrupt snapshot, rebuilding: {}", e.getMessage());
            }
        }
        return refresh();
    }

    @EventListener(ApplicationReadyEvent.class)
    public void warmOnStartup() {
        refresh();
    }

    public List<GameRoom> refresh() {
        List<GameRoom> rooms = loadFromRepository();
        try {
            redisTemplate.opsForValue().set(CACHE_KEY, objectMapper.writeValueAsString(rooms));
            log.info("[LOBBY CACHE] refreshed | totalRooms={}", rooms.size());
        } catch (Exception e) {
            log.error("[LOBBY CACHE] failed to write snapshot: {}", e.getMessage());
        }
        return rooms;
    }

    private List<GameRoom> loadFromRepository() {
        return StreamSupport.stream(gameRoomRepository.findAll().spliterator(), false)
                .collect(Collectors.toCollection(ArrayList::new));
    }
}
