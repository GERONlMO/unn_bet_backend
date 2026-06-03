package ru.meowmure.game.service;

import org.awaitility.Awaitility;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;
import ru.meowmure.game.model.Card;
import ru.meowmure.game.model.GameRoom;
import ru.meowmure.game.model.GameRoomRepository;
import ru.meowmure.game.model.PlayerState;
import ru.meowmure.game.support.TestFixtures;

import java.time.Duration;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GameServiceTest {

    @Mock
    private GameRoomRepository gameRoomRepository;

    @Mock
    private KafkaTemplate<String, String> kafkaTemplate;

    @Mock
    private CombinationObserver combinationObserver;

    @Mock
    private SseNotificationService sseNotificationService;

    @Mock
    private WalletClient walletClient;

    @Mock
    private LobbyRoomsCacheService lobbyRoomsCache;

    @InjectMocks
    private GameService gameService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(gameService, "scheduledRoundTransitions",
                java.util.concurrent.ConcurrentHashMap.<String>newKeySet());
    }

    @Test
    void getRoomStateNotFound() {
        when(gameRoomRepository.findById("missing")).thenReturn(Optional.empty());
        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> gameService.getRoomState("missing"));
        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
    }

    @Test
    void finishGameSendsKafkaMessages() {
        GameRoom room = TestFixtures.inProgressTwoPlayerRoom("r1");
        gameService.finishGame(room, "alice,bob", 100L, "Pair");

        assertEquals("FINISHED", room.getStatus());
        verify(kafkaTemplate, times(2)).send(eq("game-results"), org.mockito.ArgumentMatchers.argThat(
                msg -> msg != null && !msg.startsWith("profile:")));
        verify(kafkaTemplate).send(eq("game-results"), org.mockito.ArgumentMatchers.argThat(
                msg -> msg != null && msg.startsWith("profile:")));
        verify(kafkaTemplate).flush();
        verify(gameRoomRepository).save(room);
    }

    @Test
    void finishGameSkipsKafkaWhenZeroPot() {
        GameRoom room = TestFixtures.inProgressTwoPlayerRoom("r1");
        gameService.finishGame(room, "alice", 0L, "Fold");

        verify(kafkaTemplate, never()).send(anyString(), anyString());
    }

    @Test
    void resetRoomClearsGameState() {
        GameRoom room = TestFixtures.inProgressTwoPlayerRoom("r1");
        room.setWinner("alice");
        when(gameRoomRepository.findById("r1")).thenReturn(Optional.of(room));
        when(gameRoomRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        GameRoom reset = gameService.resetRoom("r1");

        assertEquals("WAITING", reset.getStatus());
        assertEquals(0L, reset.getPot());
        assertNull(reset.getWinner());
        assertTrue(reset.getReadyStatus().values().stream().noneMatch(Boolean::booleanValue));
    }

    @Test
    void startGameRequiresTwoSeatedPlayers() {
        GameRoom room = TestFixtures.waitingRoom("r1");
        when(gameRoomRepository.findById("r1")).thenReturn(Optional.of(room));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> gameService.startGame("r1"));
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
    }

    @Test
    void startGameInitializesGameForTwoPlayers() {
        GameRoom room = TestFixtures.seatedRoom("r1");
        when(gameRoomRepository.findById("r1")).thenReturn(Optional.of(room));
        when(gameRoomRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(walletClient.getBalance("alice")).thenReturn(1000L);
        when(walletClient.getBalance("bob")).thenReturn(1000L);

        GameRoom started = gameService.startGame("r1");

        assertEquals("IN_PROGRESS", started.getStatus());
        assertEquals(2, started.getPlayerStates().size());
        assertNotNull(started.getMainDeck());
        assertNotNull(started.getTableDeck());
        assertEquals("alice", started.getCurrentTurn());
        verify(walletClient, atLeastOnce()).deductBalance(anyString(), anyLong());
    }

    @Test
    void startGameThreePlayersFirstTurnAfterBigBlind() {
        GameRoom room = TestFixtures.seatedRoom("r1");
        room.getSeats().put(2, "carol");
        room.getPlayers().add("carol");
        when(gameRoomRepository.findById("r1")).thenReturn(Optional.of(room));
        when(gameRoomRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(walletClient.getBalance(anyString())).thenReturn(1000L);

        GameRoom started = gameService.startGame("r1");

        assertEquals("carol", started.getCurrentTurn());
    }

    @Test
    void foldEndsGameWhenOnePlayerLeft() {
        GameRoom room = TestFixtures.inProgressTwoPlayerRoom("r1");
        when(gameRoomRepository.findById("r1")).thenReturn(Optional.of(room));
        when(gameRoomRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        GameRoom result = gameService.fold("r1", "alice");

        assertEquals("FINISHED", result.getStatus());
        assertEquals("bob", result.getWinner());
    }

    @Test
    void foldRejectsWhenGameNotInProgress() {
        GameRoom room = TestFixtures.waitingRoom("r1");
        room.setStatus("WAITING");
        room.setCurrentTurn("alice");
        when(gameRoomRepository.findById("r1")).thenReturn(Optional.of(room));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> gameService.fold("r1", "alice"));
        assertEquals(HttpStatus.CONFLICT, ex.getStatusCode());
    }

    @Test
    void startGameRoomNotFound() {
        when(gameRoomRepository.findById("missing")).thenReturn(Optional.empty());
        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> gameService.startGame("missing"));
        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
    }

    @Test
    void checkTimeoutsHandlesFoldFailure() {
        GameRoom room = TestFixtures.inProgressTwoPlayerRoom("r1");
        room.setTurnStartTime(System.currentTimeMillis() - 60_000L);
        when(gameRoomRepository.findAll()).thenReturn(List.of(room));
        when(gameRoomRepository.findById("r1")).thenReturn(Optional.empty());

        gameService.checkTimeouts();

        verify(sseNotificationService, never()).notifyRoom(anyString(), any());
    }

    @Test
    void checkTimeoutsSchedulesResetWhenGameFinishes() {
        GameRoom room = TestFixtures.inProgressTwoPlayerRoom("r1");
        room.setTurnStartTime(System.currentTimeMillis() - 60_000L);
        when(gameRoomRepository.findAll()).thenReturn(List.of(room));
        when(gameRoomRepository.findById("r1")).thenReturn(Optional.of(room));
        when(gameRoomRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        gameService.checkTimeouts();

        Awaitility.await().atMost(Duration.ofSeconds(10))
                .untilAsserted(() -> verify(gameRoomRepository, atLeast(2)).save(any()));
    }

    @Test
    void advanceRoundAfterTransitionNoOpWhenRoomMissing() {
        when(gameRoomRepository.findById("missing")).thenReturn(Optional.empty());
        ReflectionTestUtils.invokeMethod(gameService, "advanceRoundAfterTransition", "missing");
        verify(sseNotificationService, never()).notifyRoom(anyString(), any());
    }

    @Test
    void scheduleRoundAdvanceSkipsDuplicateScheduling() {
        java.util.Set<String> scheduled = java.util.concurrent.ConcurrentHashMap.newKeySet();
        scheduled.add("r1");
        ReflectionTestUtils.setField(gameService, "scheduledRoundTransitions", scheduled);
        ReflectionTestUtils.invokeMethod(gameService, "scheduleRoundAdvance", "r1");
        // no exception means duplicate was skipped
    }

    @Test
    void callWithZeroAmountToCallStillMarksActed() {
        GameRoom room = TestFixtures.inProgressTwoPlayerRoom("r1");
        room.getPlayerStates().get("alice").setCurrentBet(10L);
        room.setCurrentHighestBet(10L);
        when(gameRoomRepository.findById("r1")).thenReturn(Optional.of(room));
        when(gameRoomRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        GameRoom result = gameService.call("r1", "alice");

        assertTrue(result.getPlayerStates().get("alice").isActedThisRound());
        verify(walletClient, never()).deductBalance(anyString(), anyLong());
    }

    @Test
    void foldRejectsWrongTurn() {
        GameRoom room = TestFixtures.inProgressTwoPlayerRoom("r1");
        when(gameRoomRepository.findById("r1")).thenReturn(Optional.of(room));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> gameService.fold("r1", "bob"));
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
    }

    @Test
    void foldRejectsDuringRoundTransition() {
        GameRoom room = TestFixtures.inProgressTwoPlayerRoom("r1");
        room.setRoundTransitionPending(true);
        when(gameRoomRepository.findById("r1")).thenReturn(Optional.of(room));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> gameService.fold("r1", "alice"));
        assertEquals(HttpStatus.CONFLICT, ex.getStatusCode());
    }

    @Test
    void raiseValidations() {
        GameRoom room = TestFixtures.inProgressThreePlayerRoom("r1");
        when(gameRoomRepository.findById("r1")).thenReturn(Optional.of(room));

        assertEquals(HttpStatus.BAD_REQUEST, assertThrows(ResponseStatusException.class,
                () -> gameService.raise("r1", "bob", 10L)).getStatusCode());

        room.setCurrentTurn("carol");
        when(walletClient.getBalance("carol")).thenReturn(5L);
        assertEquals(HttpStatus.BAD_REQUEST, assertThrows(ResponseStatusException.class,
                () -> gameService.raise("r1", "carol", 100L)).getStatusCode());

        when(walletClient.getBalance("carol")).thenReturn(1000L);
        assertEquals(HttpStatus.BAD_REQUEST, assertThrows(ResponseStatusException.class,
                () -> gameService.raise("r1", "carol", 5L)).getStatusCode());
    }

    @Test
    void raiseSuccess() {
        GameRoom room = TestFixtures.inProgressThreePlayerRoom("r1");
        when(gameRoomRepository.findById("r1")).thenReturn(Optional.of(room));
        when(gameRoomRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(walletClient.getBalance("carol")).thenReturn(1000L);

        GameRoom result = gameService.raise("r1", "carol", 20L);

        assertEquals(20L, result.getPlayerStates().get("carol").getCurrentBet());
        verify(walletClient).deductBalance("carol", 20L);
    }

    @Test
    void raiseAllInWhenRaisingFullBalance() {
        GameRoom room = TestFixtures.inProgressThreePlayerRoom("r1");
        when(gameRoomRepository.findById("r1")).thenReturn(Optional.of(room));
        when(gameRoomRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(walletClient.getBalance("carol")).thenReturn(15L);

        GameRoom result = gameService.raise("r1", "carol", 15L);

        assertTrue(result.getPlayerStates().get("carol").isAllIn());
    }

    @Test
    void checkRejectsWhenMustCall() {
        GameRoom room = TestFixtures.inProgressTwoPlayerRoom("r1");
        when(gameRoomRepository.findById("r1")).thenReturn(Optional.of(room));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> gameService.check("r1", "alice"));
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
    }

    @Test
    void callDeductsAndMarksActed() {
        GameRoom room = TestFixtures.inProgressTwoPlayerRoom("r1");
        when(gameRoomRepository.findById("r1")).thenReturn(Optional.of(room));
        when(gameRoomRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(walletClient.getBalance("alice")).thenReturn(1000L);

        GameRoom result = gameService.call("r1", "alice");

        assertEquals(10L, result.getPlayerStates().get("alice").getCurrentBet());
        assertTrue(result.getPlayerStates().get("alice").isActedThisRound());
        verify(walletClient).deductBalance("alice", 5L);
    }

    @Test
    void callAllInWhenInsufficientBalance() {
        GameRoom room = TestFixtures.inProgressTwoPlayerRoom("r1");
        when(gameRoomRepository.findById("r1")).thenReturn(Optional.of(room));
        when(gameRoomRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(walletClient.getBalance("alice")).thenReturn(3L);

        GameRoom result = gameService.call("r1", "alice");

        assertTrue(result.getPlayerStates().get("alice").isAllIn());
        verify(walletClient).deductBalance("alice", 3L);
    }

    @Test
    void roundEndsWithTransitionPendingAfterMatchingBets() {
        GameRoom room = TestFixtures.inProgressTwoPlayerRoom("r1");
        room.getPlayerStates().get("alice").setActedThisRound(false);
        when(gameRoomRepository.findById("r1")).thenReturn(Optional.of(room));
        when(gameRoomRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(walletClient.getBalance("alice")).thenReturn(1000L);

        GameRoom result = gameService.call("r1", "alice");

        assertTrue(result.getRoundTransitionPending());
        assertNull(result.getCurrentTurn());
    }

    @Test
    void checkTimeoutsAutoFoldsExpiredPlayer() {
        GameRoom room = TestFixtures.inProgressTwoPlayerRoom("r1");
        room.setTurnStartTime(System.currentTimeMillis() - 60_000L);
        when(gameRoomRepository.findAll()).thenReturn(List.of(room));
        when(gameRoomRepository.findById("r1")).thenReturn(Optional.of(room));
        when(gameRoomRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        gameService.checkTimeouts();

        verify(sseNotificationService).notifyRoom(eq("r1"), any());
    }

    @Test
    void checkTimeoutsIgnoresRoomsWithoutActiveTurn() {
        GameRoom room = TestFixtures.inProgressTwoPlayerRoom("r1");
        room.setRoundTransitionPending(true);
        when(gameRoomRepository.findAll()).thenReturn(List.of(room));

        gameService.checkTimeouts();

        verify(sseNotificationService, never()).notifyRoom(anyString(), any());
    }

    @Test
    void advanceRoundAfterTransitionRunsShowdown() {
        GameRoom room = TestFixtures.inProgressTwoPlayerRoom("r1");
        room.setCurrentRound(3);
        room.setRoundTransitionPending(true);
        when(gameRoomRepository.findById("r1")).thenReturn(Optional.of(room));
        when(gameRoomRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        CombinationObserver.HandResult hr = new CombinationObserver.HandResult("alice", 2, "Pair", List.of(14));
        when(combinationObserver.evaluateAllHands(anyList(), anyList()))
                .thenReturn(List.of(hr));
        when(combinationObserver.getWinners(anyList())).thenReturn(List.of(hr));

        ReflectionTestUtils.invokeMethod(gameService, "advanceRoundAfterTransition", "r1");

        verify(sseNotificationService).notifyRoom(eq("r1"), any());
        assertEquals("FINISHED", room.getStatus());
    }

    @Test
    void advanceRoundAfterTransitionNoOpWhenNotPending() {
        GameRoom room = TestFixtures.inProgressTwoPlayerRoom("r1");
        room.setRoundTransitionPending(false);
        when(gameRoomRepository.findById("r1")).thenReturn(Optional.of(room));

        ReflectionTestUtils.invokeMethod(gameService, "advanceRoundAfterTransition", "r1");

        verify(sseNotificationService, never()).notifyRoom(anyString(), any());
    }

    @Test
    void scheduledRoundAdvanceEventuallyNotifies() {
        GameRoom room = TestFixtures.inProgressTwoPlayerRoom("r1");
        room.getPlayerStates().get("alice").setActedThisRound(false);
        when(gameRoomRepository.findById("r1")).thenReturn(Optional.of(room));
        when(gameRoomRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(walletClient.getBalance("alice")).thenReturn(1000L);

        gameService.call("r1", "alice");

        Awaitility.await().atMost(Duration.ofSeconds(3))
                .untilAsserted(() -> verify(sseNotificationService, atLeastOnce()).notifyRoom(eq("r1"), any()));
    }

    @Test
    void raiseRejectsWhenOnlyOnePlayerCanAct() {
        GameRoom room = TestFixtures.inProgressTwoPlayerRoom("r1");
        room.getPlayerStates().get("bob").setAllIn(true);
        room.setCurrentTurn("alice");
        when(gameRoomRepository.findById("r1")).thenReturn(Optional.of(room));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> gameService.raise("r1", "alice", 10L));
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
    }

    @Test
    void checkSucceedsWhenBetMatched() {
        GameRoom room = TestFixtures.inProgressThreePlayerRoom("r1");
        room.getPlayerStates().get("carol").setCurrentBet(10L);
        when(gameRoomRepository.findById("r1")).thenReturn(Optional.of(room));
        when(gameRoomRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        GameRoom result = gameService.check("r1", "carol");

        assertTrue(result.getPlayerStates().get("carol").isActedThisRound());
    }

    @Test
    void applyRoundAdvanceFastForwardsWhenAllAllIn() {
        GameRoom room = TestFixtures.inProgressTwoPlayerRoom("r1");
        room.setCurrentRound(0);
        room.getPlayerStates().values().forEach(p -> {
            p.setAllIn(true);
            p.setActedThisRound(true);
        });
        when(gameRoomRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        CombinationObserver.HandResult hr = new CombinationObserver.HandResult("bob", 1, "High Card", List.of(13));
        when(combinationObserver.evaluateAllHands(anyList(), anyList())).thenReturn(List.of(hr));
        when(combinationObserver.getWinners(anyList())).thenReturn(List.of(hr));

        ReflectionTestUtils.invokeMethod(gameService, "applyRoundAdvance", room);

        assertEquals("FINISHED", room.getStatus());
    }
}
