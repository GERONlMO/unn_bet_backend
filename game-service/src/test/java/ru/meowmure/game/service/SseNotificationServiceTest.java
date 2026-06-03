package ru.meowmure.game.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import ru.meowmure.game.model.GameRoom;
import ru.meowmure.game.support.TestFixtures;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SseNotificationServiceTest {

    @Mock
    private GameRoomViewService gameRoomViewService;

    @InjectMocks
    private SseNotificationService sseNotificationService;

    @Test
    void subscribeReturnsEmitterAndTracksUser() {
        SseEmitter emitter = sseNotificationService.subscribe("r1", "alice");
        assertNotNull(emitter);
    }

    @Test
    void notifyRoomDoesNothingWithoutSubscribers() {
        GameRoom room = TestFixtures.waitingRoom("r1");
        sseNotificationService.notifyRoom("r1", room);
        verifyNoInteractions(gameRoomViewService);
    }

    @Test
    void notifyRoomSendsUpdateToSubscribers() throws Exception {
        GameRoom room = TestFixtures.waitingRoom("r1");
        GameRoom view = room.createViewForUser("alice");
        when(gameRoomViewService.createViewForUser(room, "alice")).thenReturn(view);

        sseNotificationService.subscribe("r1", "alice");
        sseNotificationService.notifyRoom("r1", room);

        verify(gameRoomViewService).createViewForUser(room, "alice");
    }

    @Test
    void notifyRoomLogsWhenNoSubscribers() {
        GameRoom room = TestFixtures.waitingRoom("r1");
        sseNotificationService.notifyRoom("empty-room", room);
        verifyNoInteractions(gameRoomViewService);
    }

    @Test
    void notifyRoomRemovesFailedEmitter() throws Exception {
        GameRoom room = TestFixtures.waitingRoom("r1");
        when(gameRoomViewService.createViewForUser(any(), any())).thenThrow(new RuntimeException("send failed"));

        sseNotificationService.subscribe("r1", "alice");
        sseNotificationService.notifyRoom("r1", room);

        verify(gameRoomViewService).createViewForUser(room, "alice");
    }

    @Test
    void subscribeRegistersCompletionHandlers() {
        SseEmitter emitter = sseNotificationService.subscribe("r1", "alice");
        assertNotNull(emitter);
        emitter.complete();
    }
}
