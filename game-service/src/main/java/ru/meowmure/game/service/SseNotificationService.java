package ru.meowmure.game.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import ru.meowmure.game.model.GameRoom;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class SseNotificationService {

    private static final Logger log = LoggerFactory.getLogger(SseNotificationService.class);

    private final Map<String, Map<String, SseEmitter>> roomEmitters = new ConcurrentHashMap<>();

    @Autowired
    private GameRoomViewService gameRoomViewService;

    public SseEmitter subscribe(String roomId, String username) {
        SseEmitter emitter = new SseEmitter(3600000L); // 1 hour timeout
        
        roomEmitters.putIfAbsent(roomId, new ConcurrentHashMap<>());
        roomEmitters.get(roomId).put(username, emitter);

        int subscribers = roomEmitters.get(roomId).size();
        log.info("[SSE SUBSCRIBE] room={} user={} | totalSubscribers={}", roomId, username, subscribers);
        
        emitter.onCompletion(() -> {
            roomEmitters.get(roomId).remove(username);
            log.info("[SSE DISCONNECT] room={} user={} | reason=completion | remaining={}",
                    roomId, username, roomEmitters.getOrDefault(roomId, Map.of()).size());
        });
        emitter.onTimeout(() -> {
            roomEmitters.get(roomId).remove(username);
            log.info("[SSE DISCONNECT] room={} user={} | reason=timeout | remaining={}",
                    roomId, username, roomEmitters.getOrDefault(roomId, Map.of()).size());
        });
        
        return emitter;
    }

    public void notifyRoom(String roomId, GameRoom room) {
        Map<String, SseEmitter> emitters = roomEmitters.get(roomId);
        if (emitters == null || emitters.isEmpty()) {
            log.info("[SSE NOTIFY] room={} | status={} round={} | no subscribers",
                    roomId, room != null ? room.getStatus() : "?", room != null ? room.getCurrentRound() : "?");
            return;
        }

        log.info("[SSE NOTIFY] room={} | status={} round={} turn={} pot={} | recipients={}",
                roomId, room.getStatus(), room.getCurrentRound(), room.getCurrentTurn(), room.getPot(), emitters.keySet());

        emitters.forEach((user, emitter) -> {
            try {
                GameRoom userView = gameRoomViewService.createViewForUser(room, user);
                emitter.send(SseEmitter.event().name("UPDATE").data(userView));
                log.debug("[SSE SENT] room={} recipient={} event=UPDATE", roomId, user);
            } catch (Exception e) {
                log.warn("[SSE SEND FAILED] room={} recipient={} | error={}", roomId, user, e.getMessage());
                emitter.complete();
                emitters.remove(user);
            }
        });
    }
}