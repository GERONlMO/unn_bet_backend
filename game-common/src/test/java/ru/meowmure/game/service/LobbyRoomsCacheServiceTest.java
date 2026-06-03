package ru.meowmure.game.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import ru.meowmure.game.model.GameRoom;
import ru.meowmure.game.model.GameRoomRepository;
import ru.meowmure.game.support.TestFixtures;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LobbyRoomsCacheServiceTest {

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private GameRoomRepository gameRoomRepository;

    @Mock
    private ValueOperations<String, String> valueOps;

    private LobbyRoomsCacheService cacheService;

    @BeforeEach
    void setUp() {
        lenient().when(redisTemplate.opsForValue()).thenReturn(valueOps);
        cacheService = new LobbyRoomsCacheService(redisTemplate, gameRoomRepository, new ObjectMapper());
    }

    @Test
    void getAllReturnsCachedSnapshot() throws Exception {
        GameRoom room = TestFixtures.waitingRoom("r1");
        String json = new ObjectMapper().writeValueAsString(List.of(room));
        when(valueOps.get(LobbyRoomsCacheService.CACHE_KEY)).thenReturn(json);

        List<GameRoom> rooms = cacheService.getAll();

        assertEquals(1, rooms.size());
        assertEquals("r1", rooms.get(0).getRoomId());
        verify(gameRoomRepository, never()).findAll();
    }

    @Test
    void getAllRebuildsWhenCacheMiss() {
        GameRoom room = TestFixtures.waitingRoom("r1");
        when(valueOps.get(LobbyRoomsCacheService.CACHE_KEY)).thenReturn(null);
        when(gameRoomRepository.findAll()).thenReturn(List.of(room));

        List<GameRoom> rooms = cacheService.getAll();

        assertEquals(1, rooms.size());
        verify(valueOps).set(eq(LobbyRoomsCacheService.CACHE_KEY), anyString());
    }

    @Test
    void refreshLoadsFromRepositoryAndWritesCache() {
        GameRoom room = TestFixtures.waitingRoom("r1");
        when(gameRoomRepository.findAll()).thenReturn(List.of(room));

        List<GameRoom> rooms = cacheService.refresh();

        assertSame(room, rooms.get(0));
        verify(valueOps).set(eq(LobbyRoomsCacheService.CACHE_KEY), anyString());
    }
}
