package ru.meowmure.game.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import ru.meowmure.game.model.GameRoom;
import ru.meowmure.game.model.GameRoomRepository;
import ru.meowmure.game.service.GameRoomViewService;
import ru.meowmure.game.service.GameService;
import ru.meowmure.game.service.SseNotificationService;
import ru.meowmure.game.support.TestFixtures;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(GameController.class)
@TestPropertySource(properties = {
        "internal.service.token=test-token",
        "spring.profiles.active=webmvc-test"
})
class GameControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private GameService gameService;

    @MockBean
    private SseNotificationService sseNotificationService;

    @MockBean
    private GameRoomViewService gameRoomViewService;

    @MockBean
    private GameRoomRepository gameRoomRepository;

    @Test
    void getRoomStateReturnsView() throws Exception {
        GameRoom room = TestFixtures.waitingRoom("r1");
        GameRoom view = room.createViewForUser("alice");
        when(gameService.getRoomState("r1")).thenReturn(room);
        when(gameRoomViewService.createViewForUser(room, "alice")).thenReturn(view);

        mockMvc.perform(get("/api/game/room/r1").header("loggedInUser", "alice"))
                .andExpect(status().isOk());
    }

    @Test
    void foldRequiresAuthentication() throws Exception {
        mockMvc.perform(post("/api/game/fold").param("roomId", "r1"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void foldInvokesServiceAndNotifies() throws Exception {
        GameRoom room = TestFixtures.inProgressTwoPlayerRoom("r1");
        when(gameService.fold("r1", "alice")).thenReturn(room);

        mockMvc.perform(post("/api/game/fold")
                        .header("loggedInUser", "alice")
                        .param("roomId", "r1"))
                .andExpect(status().isOk());

        verify(sseNotificationService).notifyRoom("r1", room);
    }

    @Test
    void raiseInvokesService() throws Exception {
        GameRoom room = TestFixtures.inProgressTwoPlayerRoom("r1");
        when(gameService.raise("r1", "alice", 20L)).thenReturn(room);

        mockMvc.perform(post("/api/game/raise")
                        .header("loggedInUser", "alice")
                        .param("roomId", "r1")
                        .param("amount", "20"))
                .andExpect(status().isOk());
    }

    @Test
    void checkInvokesService() throws Exception {
        GameRoom room = TestFixtures.inProgressTwoPlayerRoom("r1");
        when(gameService.check("r1", "alice")).thenReturn(room);

        mockMvc.perform(post("/api/game/check")
                        .header("loggedInUser", "alice")
                        .param("roomId", "r1"))
                .andExpect(status().isOk());
    }

    @Test
    void callInvokesService() throws Exception {
        GameRoom room = TestFixtures.inProgressTwoPlayerRoom("r1");
        when(gameService.call("r1", "alice")).thenReturn(room);

        mockMvc.perform(post("/api/game/call")
                        .header("loggedInUser", "alice")
                        .param("roomId", "r1"))
                .andExpect(status().isOk());
    }

    @Test
    void streamRequiresAuthentication() throws Exception {
        mockMvc.perform(get("/api/game/stream/r1"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void streamSendsInitEvent() throws Exception {
        GameRoom room = TestFixtures.waitingRoom("r1");
        GameRoom view = room.createViewForUser("alice");
        SseEmitter emitter = new SseEmitter();
        when(sseNotificationService.subscribe("r1", "alice")).thenReturn(emitter);
        when(gameService.getRoomState("r1")).thenReturn(room);
        when(gameRoomViewService.createViewForUser(room, "alice")).thenReturn(view);

        mockMvc.perform(get("/api/game/stream/r1").header("loggedInUser", "alice"))
                .andExpect(status().isOk());
    }

    @Test
    void foldOnFinishedGameSchedulesReset() throws Exception {
        GameRoom room = TestFixtures.inProgressTwoPlayerRoom("r1");
        room.setStatus("FINISHED");
        when(gameService.fold("r1", "alice")).thenReturn(room);

        mockMvc.perform(post("/api/game/fold")
                        .header("loggedInUser", "alice")
                        .param("roomId", "r1"))
                .andExpect(status().isOk());

        verify(sseNotificationService).notifyRoom("r1", room);
    }
}
