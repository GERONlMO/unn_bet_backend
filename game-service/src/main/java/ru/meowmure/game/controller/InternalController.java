package ru.meowmure.game.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.meowmure.game.model.GameRoom;
import ru.meowmure.game.service.GameService;
import ru.meowmure.game.service.SseNotificationService;

@RestController
@RequestMapping("/internal")
public class InternalController {

    @Autowired
    private GameService gameService;

    @Autowired
    private SseNotificationService sseNotificationService;

    @PostMapping("/room/{roomId}/notify")
    public void notifyRoom(@PathVariable String roomId) {
        GameRoom room = gameService.getRoomState(roomId);
        sseNotificationService.notifyRoom(roomId, room);
    }

    @PostMapping("/game/{roomId}/start")
    public GameRoom startGame(@PathVariable String roomId) {
        GameRoom room = gameService.startGame(roomId);
        sseNotificationService.notifyRoom(roomId, room);
        return room;
    }
}
