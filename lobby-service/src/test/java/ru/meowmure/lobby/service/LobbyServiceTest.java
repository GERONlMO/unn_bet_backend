package ru.meowmure.lobby.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import ru.meowmure.game.model.GameRoom;
import ru.meowmure.game.model.GameRoomRepository;
import ru.meowmure.game.service.LobbyRoomsCacheService;
import ru.meowmure.lobby.client.GameServiceClient;
import ru.meowmure.lobby.support.TestFixtures;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.lenient;

@ExtendWith(MockitoExtension.class)
class LobbyServiceTest {

    @Mock
    private GameRoomRepository gameRoomRepository;

    @Mock
    private GameServiceClient gameServiceClient;

    @Mock
    private LobbyRoomsCacheService lobbyRoomsCache;

    @InjectMocks
    private LobbyService lobbyService;

    @BeforeEach
    void setUp() {
        lenient().when(gameRoomRepository.findAll()).thenReturn(List.of());
    }

    @Test
    void createRoomSuccess() {
        when(gameRoomRepository.save(any(GameRoom.class))).thenAnswer(inv -> inv.getArgument(0));

        GameRoom room = lobbyService.createRoom("My Room", false, "", 4, 10L, 30000L);

        assertEquals("My Room", room.getName());
        assertEquals(4, room.getMaxPlayers());
        assertEquals(10L, room.getMinBet());
        verify(gameRoomRepository).save(any(GameRoom.class));
    }

    @Test
    void createRoomRejectsEmptyName() {
        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> lobbyService.createRoom("  ", false, "", 6, 10L, 30000L));
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
    }

    @Test
    void createRoomRejectsLongName() {
        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> lobbyService.createRoom("a".repeat(25), false, "", 6, 10L, 30000L));
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
    }

    @Test
    void createRoomRejectsInvalidCharacters() {
        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> lobbyService.createRoom("bad@name", false, "", 6, 10L, 30000L));
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
    }

    @Test
    void createRoomRejectsDuplicateName() {
        GameRoom existing = TestFixtures.waitingRoom("existing");
        existing.setName("Taken");
        when(gameRoomRepository.findAll()).thenReturn(List.of(existing));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> lobbyService.createRoom("Taken", false, "", 6, 10L, 30000L));
        assertEquals(HttpStatus.CONFLICT, ex.getStatusCode());
    }

    @Test
    void createRoomNormalizesInvalidSettings() {
        when(gameRoomRepository.save(any(GameRoom.class))).thenAnswer(inv -> inv.getArgument(0));

        GameRoom room = lobbyService.createRoom("Room", false, "", 1, 999L, 5000L);

        assertEquals(6, room.getMaxPlayers());
        assertEquals(10L, room.getMinBet());
        assertEquals(30000L, room.getTurnTimeLimit());
    }

    @Test
    void joinRoomByNameNotFound() {
        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> lobbyService.joinRoomByName("missing", "alice", ""));
        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
    }

    @Test
    void joinRoomSuccess() {
        GameRoom room = TestFixtures.waitingRoom("r1");
        when(gameRoomRepository.findById("r1")).thenReturn(Optional.of(room));
        when(gameRoomRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        GameRoom joined = lobbyService.joinRoom("r1", "carol", "");

        assertTrue(joined.getPlayers().contains("carol"));
        assertFalse(joined.getReadyStatus().get("carol"));
    }

    @Test
    void joinRoomWhenAlreadyMember() {
        GameRoom room = TestFixtures.waitingRoom("r1");
        when(gameRoomRepository.findById("r1")).thenReturn(Optional.of(room));

        GameRoom joined = lobbyService.joinRoom("r1", "alice", "");

        assertEquals(2, joined.getPlayers().size());
        verify(gameRoomRepository, never()).save(any());
    }

    @Test
    void joinRoomRejectsWhenGameInProgress() {
        GameRoom room = TestFixtures.waitingRoom("r1");
        room.setStatus("IN_PROGRESS");
        when(gameRoomRepository.findById("r1")).thenReturn(Optional.of(room));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> lobbyService.joinRoom("r1", "carol", ""));
        assertEquals(HttpStatus.CONFLICT, ex.getStatusCode());
    }

    @Test
    void joinRoomRejectsWrongPassword() {
        GameRoom room = TestFixtures.waitingRoom("r1");
        room.setPrivate(true);
        room.setPassword("secret");
        when(gameRoomRepository.findById("r1")).thenReturn(Optional.of(room));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> lobbyService.joinRoom("r1", "carol", "wrong"));
        assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
    }

    @Test
    void joinRoomRejectsWhenFull() {
        GameRoom room = new GameRoom("r1", "Full", false, "", 2, 10L, 30000L);
        room.setPlayers(new java.util.ArrayList<>(List.of("alice", "bob")));
        when(gameRoomRepository.findById("r1")).thenReturn(Optional.of(room));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> lobbyService.joinRoom("r1", "carol", ""));
        assertEquals(HttpStatus.CONFLICT, ex.getStatusCode());
    }

    @Test
    void joinRoomKicksFromOldRoom() {
        GameRoom oldRoom = TestFixtures.waitingRoom("old");
        oldRoom.setName("Old");
        GameRoom newRoom = TestFixtures.waitingRoom("new");
        newRoom.setName("New");

        when(gameRoomRepository.findAll()).thenReturn(List.of(oldRoom, newRoom));
        when(gameRoomRepository.findById("old")).thenReturn(Optional.of(oldRoom));
        when(gameRoomRepository.findById("new")).thenReturn(Optional.of(newRoom));
        when(gameRoomRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        lobbyService.joinRoom("new", "alice", "");

        verify(gameServiceClient).notifyRoom("old");
    }

    @Test
    void takeSeatSuccess() {
        GameRoom room = TestFixtures.waitingRoom("r1");
        when(gameRoomRepository.findById("r1")).thenReturn(Optional.of(room));
        when(gameRoomRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        GameRoom updated = lobbyService.takeSeat("r1", "alice", 0);

        assertEquals("alice", updated.getSeats().get(0));
    }

    @Test
    void takeSeatRejectsIfNotJoined() {
        GameRoom room = TestFixtures.waitingRoom("r1");
        when(gameRoomRepository.findById("r1")).thenReturn(Optional.of(room));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> lobbyService.takeSeat("r1", "unknown", 0));
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
    }

    @Test
    void takeSeatRejectsInvalidSeat() {
        GameRoom room = TestFixtures.waitingRoom("r1");
        when(gameRoomRepository.findById("r1")).thenReturn(Optional.of(room));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> lobbyService.takeSeat("r1", "alice", 99));
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
    }

    @Test
    void takeSeatRejectsTakenSeat() {
        GameRoom room = TestFixtures.seatedRoom("r1");
        when(gameRoomRepository.findById("r1")).thenReturn(Optional.of(room));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> lobbyService.takeSeat("r1", "bob", 0));
        assertEquals(HttpStatus.CONFLICT, ex.getStatusCode());
    }

    @Test
    void leaveRoomDeletesEmptyRoom() {
        GameRoom room = TestFixtures.waitingRoom("r1");
        room.setPlayers(new java.util.ArrayList<>(List.of("alice")));
        when(gameRoomRepository.findById("r1")).thenReturn(Optional.of(room));

        GameRoom result = lobbyService.leaveRoom("r1", "alice");

        assertNull(result);
        verify(gameRoomRepository).delete(room);
    }

    @Test
    void leaveRoomUpdatesRemainingPlayers() {
        GameRoom room = TestFixtures.seatedRoom("r1");
        when(gameRoomRepository.findById("r1")).thenReturn(Optional.of(room));
        when(gameRoomRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        GameRoom result = lobbyService.leaveRoom("r1", "alice");

        assertNotNull(result);
        assertFalse(result.getPlayers().contains("alice"));
        assertFalse(result.getSeats().containsValue("alice"));
    }

    @Test
    void leaveRoomWhenUserNotInRoom() {
        GameRoom room = TestFixtures.waitingRoom("r1");
        when(gameRoomRepository.findById("r1")).thenReturn(Optional.of(room));

        GameRoom result = lobbyService.leaveRoom("r1", "ghost");

        assertSame(room, result);
    }

    @Test
    void setReadyRejectsWhenGameInProgress() {
        GameRoom room = TestFixtures.seatedRoom("r1");
        room.setStatus("IN_PROGRESS");
        when(gameRoomRepository.findById("r1")).thenReturn(Optional.of(room));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> lobbyService.setReady("r1", "alice", true));
        assertEquals(HttpStatus.CONFLICT, ex.getStatusCode());
        verify(gameRoomRepository, never()).save(any());
    }

    @Test
    void setReadyRejectsWhenGameFinished() {
        GameRoom room = TestFixtures.seatedRoom("r1");
        room.setStatus("FINISHED");
        when(gameRoomRepository.findById("r1")).thenReturn(Optional.of(room));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> lobbyService.setReady("r1", "alice", false));
        assertEquals(HttpStatus.CONFLICT, ex.getStatusCode());
    }

    @Test
    void setReadyStartsGameWhenAllReady() {
        GameRoom room = TestFixtures.seatedRoom("r1");
        Map<String, Boolean> ready = new HashMap<>();
        ready.put("alice", true);
        ready.put("bob", false);
        room.setReadyStatus(ready);
        when(gameRoomRepository.findById("r1")).thenReturn(Optional.of(room));
        when(gameRoomRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(gameServiceClient.startGame("r1")).thenReturn(room);

        lobbyService.setReady("r1", "bob", true);

        verify(gameServiceClient).startGame("r1");
    }

    @Test
    void setReadyDoesNotStartWhenNotAllReady() {
        GameRoom room = TestFixtures.seatedRoom("r1");
        when(gameRoomRepository.findById("r1")).thenReturn(Optional.of(room));
        when(gameRoomRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        lobbyService.setReady("r1", "alice", true);

        verify(gameServiceClient, never()).startGame(any());
    }

    @Test
    void getAllRoomsReturnsCachedContents() {
        GameRoom room = TestFixtures.waitingRoom("r1");
        when(lobbyRoomsCache.getAll()).thenReturn(List.of(room));

        assertEquals(1, lobbyService.getAllRooms().size());
        verify(lobbyRoomsCache).getAll();
        verify(gameRoomRepository, never()).findAll();
    }

    @Test
    void deleteAllLobbiesClearsRepositoryAndCache() {
        when(gameRoomRepository.count()).thenReturn(2L);

        int deleted = lobbyService.deleteAllLobbies();

        assertEquals(2, deleted);
        verify(gameRoomRepository).deleteAll();
        verify(lobbyRoomsCache).refresh();
    }

    @Test
    void createRoomRefreshesCache() {
        when(gameRoomRepository.save(any(GameRoom.class))).thenAnswer(inv -> inv.getArgument(0));

        lobbyService.createRoom("My Room", false, "", 4, 10L, 30000L);

        verify(lobbyRoomsCache).refresh();
    }
}
