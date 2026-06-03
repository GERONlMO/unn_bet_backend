package ru.meowmure.game.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import ru.meowmure.game.model.GameRoom;
import ru.meowmure.game.service.GameService;
import ru.meowmure.game.service.GameRoomViewService;
import ru.meowmure.game.service.SseNotificationService;

@RestController
@RequestMapping("/api/game")
public class GameController {

    private static final Logger log = LoggerFactory.getLogger(GameController.class);

    @Autowired
    private GameService gameService;
    
    @Autowired
    private SseNotificationService sseNotificationService;

    @Autowired
    private GameRoomViewService gameRoomViewService;

    @GetMapping("/stream/{roomId}")
    public SseEmitter streamGameState(@PathVariable String roomId, @RequestHeader(value = "loggedInUser", required = false) String loggedInUser) {
        if (loggedInUser == null) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Not authenticated");
        
        SseEmitter emitter = sseNotificationService.subscribe(roomId, loggedInUser);
        
        // Send initial state
        try {
            GameRoom room = gameService.getRoomState(roomId);
            emitter.send(SseEmitter.event().name("INIT").data(gameRoomViewService.createViewForUser(room, loggedInUser)));
            log.info("[SSE INIT SENT] room={} user={} status={}", roomId, loggedInUser, room.getStatus());
        } catch (Exception e) {
            log.warn("[SSE INIT FAILED] room={} user={} | error={}", roomId, loggedInUser, e.getMessage());
            emitter.completeWithError(e);
        }
        
        return emitter;
    }

    @GetMapping("/room/{roomId}")
    public GameRoom getRoomState(@PathVariable String roomId, @RequestHeader(value = "loggedInUser", required = false) String loggedInUser) {
        GameRoom room = gameService.getRoomState(roomId);
        return gameRoomViewService.createViewForUser(room, loggedInUser);
    }

    private void handleGameAction(String roomId, GameRoom room, String action, String user) {
        log.info("[GAME ACTION DONE] {} by user={} room={} | status={} turn={}",
                action, user, roomId, room.getStatus(), room.getCurrentTurn());
        sseNotificationService.notifyRoom(roomId, room);
        if ("FINISHED".equals(room.getStatus())) {
            // Run reset asynchronously after a delay to let frontend show animations
            new Thread(() -> {
                try {
                    Thread.sleep(8000); // 8 seconds delay
                    GameRoom resetRoom = gameService.resetRoom(roomId);
                    sseNotificationService.notifyRoom(roomId, resetRoom);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }).start();
        }
    }

    @PostMapping("/fold")
    public void fold(@RequestHeader(value = "loggedInUser", required = false) String loggedInUser, @RequestParam String roomId) {
        if (loggedInUser == null) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Not authenticated");
        GameRoom room = gameService.fold(roomId, loggedInUser);
        handleGameAction(roomId, room, "FOLD", loggedInUser);
    }

    @PostMapping("/raise")
    public void raise(@RequestHeader(value = "loggedInUser", required = false) String loggedInUser, @RequestParam String roomId, @RequestParam Long amount) {
        if (loggedInUser == null) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Not authenticated");
        GameRoom room = gameService.raise(roomId, loggedInUser, amount);
        handleGameAction(roomId, room, "RAISE amount=" + amount, loggedInUser);
    }

    @PostMapping("/check")
    public void check(@RequestHeader(value = "loggedInUser", required = false) String loggedInUser, @RequestParam String roomId) {
        if (loggedInUser == null) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Not authenticated");
        GameRoom room = gameService.check(roomId, loggedInUser);
        handleGameAction(roomId, room, "CHECK", loggedInUser);
    }

    @PostMapping("/call")
    public void call(@RequestHeader(value = "loggedInUser", required = false) String loggedInUser, @RequestParam String roomId) {
        if (loggedInUser == null) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Not authenticated");
        GameRoom room = gameService.call(roomId, loggedInUser);
        handleGameAction(roomId, room, "CALL", loggedInUser);
    }
}
