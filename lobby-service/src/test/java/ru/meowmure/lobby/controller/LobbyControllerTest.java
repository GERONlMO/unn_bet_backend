package ru.meowmure.lobby.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.context.TestPropertySource;
import ru.meowmure.game.model.GameRoom;
import ru.meowmure.game.model.GameRoomRepository;
import ru.meowmure.game.service.GameRoomViewService;
import ru.meowmure.lobby.client.GameServiceClient;
import ru.meowmure.lobby.service.LobbyService;
import ru.meowmure.lobby.support.TestFixtures;

import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(LobbyController.class)
@TestPropertySource(properties = {
        "internal.service.token=test-token",
        "wallet.service.url=http://localhost:8082",
        "spring.profiles.active=webmvc-test"
})
class LobbyControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private LobbyService lobbyService;

    @MockBean
    private GameServiceClient gameServiceClient;

    @MockBean
    private GameRoomViewService gameRoomViewService;

    @MockBean
    private GameRoomRepository gameRoomRepository;

    @Test
    void createRoomReturnsView() throws Exception {
        GameRoom room = TestFixtures.waitingRoom("r1");
        CreateRoomRequest request = new CreateRoomRequest();
        request.setName("Room");
        request.setPrivate(false);
        request.setMaxPlayers(6);
        request.setMinBet(10L);
        request.setTurnTimeLimit(30000L);

        when(lobbyService.createRoom(anyString(), anyBoolean(), any(), anyInt(), anyLong(), anyLong()))
                .thenReturn(room);
        when(gameRoomViewService.createViewForUser(eq(room), isNull())).thenReturn(room);

        mockMvc.perform(post("/api/lobby/create")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());
    }

    @Test
    void joinRoomRequiresAuth() throws Exception {
        JoinRoomRequest request = new JoinRoomRequest();
        request.setRoomName("Room");

        mockMvc.perform(post("/api/lobby/join")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void joinRoomSuccess() throws Exception {
        GameRoom room = TestFixtures.waitingRoom("r1");
        JoinRoomRequest request = new JoinRoomRequest();
        request.setRoomName("Room");

        when(lobbyService.joinRoomByName("Room", "alice", null)).thenReturn(room);
        when(gameRoomViewService.createViewForUser(room, "alice")).thenReturn(room);

        mockMvc.perform(post("/api/lobby/join")
                        .header("loggedInUser", "alice")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        verify(gameServiceClient).notifyRoom("r1");
    }

    @Test
    void getAllRoomsReturnsList() throws Exception {
        GameRoom room = TestFixtures.waitingRoom("r1");
        when(lobbyService.getAllRooms()).thenReturn(List.of(room));
        when(gameRoomViewService.createViewForUser(room, "alice")).thenReturn(room);

        mockMvc.perform(get("/api/lobby/all").header("loggedInUser", "alice"))
                .andExpect(status().isOk());
    }

    @Test
    void leaveRoomInvokesService() throws Exception {
        GameRoom room = TestFixtures.seatedRoom("r1");
        when(lobbyService.leaveRoom("r1", "alice")).thenReturn(room);

        mockMvc.perform(post("/api/lobby/leave")
                        .header("loggedInUser", "alice")
                        .param("roomId", "r1"))
                .andExpect(status().isOk());

        verify(gameServiceClient).notifyRoom("r1");
    }

    @Test
    void leaveRoomWhenRoomDeleted() throws Exception {
        when(lobbyService.leaveRoom("r1", "alice")).thenReturn(null);

        mockMvc.perform(post("/api/lobby/leave")
                        .header("loggedInUser", "alice")
                        .param("roomId", "r1"))
                .andExpect(status().isOk());

        verify(gameServiceClient, never()).notifyRoom(anyString());
    }

    @Test
    void takeSeatRequiresAuth() throws Exception {
        mockMvc.perform(post("/api/lobby/seat").param("roomId", "r1").param("seatNumber", "0"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void deleteAllLobbiesRequiresAdmin() throws Exception {
        mockMvc.perform(delete("/api/lobby/admin/all")
                        .header("loggedInUser", "alice")
                        .header("loggedInRole", "USER"))
                .andExpect(status().isForbidden());

        verify(lobbyService, never()).deleteAllLobbies();
    }

    @Test
    void deleteAllLobbiesSuccessForAdmin() throws Exception {
        when(lobbyService.deleteAllLobbies()).thenReturn(3);

        mockMvc.perform(delete("/api/lobby/admin/all")
                        .header("loggedInUser", "admin")
                        .header("loggedInRole", "ADMIN"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.deletedRooms").value(3));

        verify(lobbyService).deleteAllLobbies();
    }

    @Test
    void setReadyInvokesService() throws Exception {
        GameRoom room = TestFixtures.seatedRoom("r1");
        when(lobbyService.setReady("r1", "alice", true)).thenReturn(room);
        when(gameRoomViewService.createViewForUser(room, "alice")).thenReturn(room);

        mockMvc.perform(post("/api/lobby/ready")
                        .header("loggedInUser", "alice")
                        .param("roomId", "r1")
                        .param("ready", "true"))
                .andExpect(status().isOk());
    }
}
