package ru.meowmure.lobby.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import ru.meowmure.game.model.GameRoom;
import ru.meowmure.game.security.UserRoles;
import ru.meowmure.game.service.GameRoomViewService;
import ru.meowmure.lobby.client.GameServiceClient;
import ru.meowmure.lobby.service.LobbyService;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/lobby")
public class LobbyController {

    @Autowired
    private LobbyService lobbyService;

    @Autowired
    private GameServiceClient gameServiceClient;

    @Autowired
    private GameRoomViewService gameRoomViewService;

    @PostMapping("/create")
    public GameRoom createRoom(@RequestHeader(value = "loggedInUser", required = false) String loggedInUser,
                               @RequestBody CreateRoomRequest request) {
        GameRoom room = lobbyService.createRoom(request.getName(), request.isPrivate(), request.getPassword(),
                request.getMaxPlayers(), request.getMinBet(), request.getTurnTimeLimit());
        return gameRoomViewService.createViewForUser(room, loggedInUser);
    }

    @PostMapping("/join")
    public GameRoom joinRoom(@RequestHeader(value = "loggedInUser", required = false) String loggedInUser,
                             @RequestBody JoinRoomRequest request) {
        if (loggedInUser == null) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Not authenticated");
        GameRoom room = lobbyService.joinRoomByName(request.getRoomName(), loggedInUser, request.getPassword());
        gameServiceClient.notifyRoom(room.getRoomId());
        return gameRoomViewService.createViewForUser(room, loggedInUser);
    }

    @PostMapping("/seat")
    public GameRoom takeSeat(@RequestHeader(value = "loggedInUser", required = false) String loggedInUser,
                             @RequestParam String roomId, @RequestParam int seatNumber) {
        if (loggedInUser == null) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Not authenticated");
        GameRoom room = lobbyService.takeSeat(roomId, loggedInUser, seatNumber);
        gameServiceClient.notifyRoom(roomId);
        return gameRoomViewService.createViewForUser(room, loggedInUser);
    }

    @PostMapping("/leave")
    public void leaveRoom(@RequestHeader(value = "loggedInUser", required = false) String loggedInUser,
                          @RequestParam String roomId) {
        if (loggedInUser == null) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Not authenticated");
        GameRoom room = lobbyService.leaveRoom(roomId, loggedInUser);
        if (room != null) {
            gameServiceClient.notifyRoom(roomId);
        }
    }

    @PostMapping("/ready")
    public GameRoom setReady(@RequestHeader(value = "loggedInUser", required = false) String loggedInUser,
                             @RequestParam String roomId,
                             @RequestParam boolean ready) {
        if (loggedInUser == null) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Not authenticated");
        GameRoom room = lobbyService.setReady(roomId, loggedInUser, ready);
        gameServiceClient.notifyRoom(roomId);
        return gameRoomViewService.createViewForUser(room, loggedInUser);
    }

    @GetMapping("/all")
    public List<GameRoom> getAllRooms(@RequestHeader(value = "loggedInUser", required = false) String loggedInUser) {
        return gameRoomViewService.createViewsForRooms(lobbyService.getAllRooms(), loggedInUser);
    }

    @DeleteMapping("/admin/all")
    public Map<String, Integer> deleteAllLobbies(
            @RequestHeader(value = "loggedInUser", required = false) String loggedInUser,
            @RequestHeader(value = "loggedInRole", required = false) String loggedInRole) {
        UserRoles.requireAuthenticated(loggedInUser);
        UserRoles.requireAdmin(loggedInRole);
        int deleted = lobbyService.deleteAllLobbies();
        return Map.of("deletedRooms", deleted);
    }
}
