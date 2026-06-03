package ru.meowmure.lobby.support;

import ru.meowmure.game.model.GameRoom;
import ru.meowmure.game.model.PlayerState;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class TestFixtures {

    private TestFixtures() {
    }

    public static GameRoom waitingRoom(String roomId) {
        GameRoom room = new GameRoom(roomId, "Test Room", false, "", 6, 10L, 30000L);
        room.setPlayers(new ArrayList<>(List.of("alice", "bob")));
        Map<String, Boolean> ready = new HashMap<>();
        ready.put("alice", false);
        ready.put("bob", false);
        room.setReadyStatus(ready);
        return room;
    }

    public static GameRoom seatedRoom(String roomId) {
        GameRoom room = waitingRoom(roomId);
        Map<Integer, String> seats = new HashMap<>();
        seats.put(0, "alice");
        seats.put(1, "bob");
        room.setSeats(seats);
        return room;
    }

    public static GameRoom inProgressRoom(String roomId) {
        GameRoom room = seatedRoom(roomId);
        room.setStatus("IN_PROGRESS");
        Map<String, PlayerState> states = new HashMap<>();
        states.put("alice", new PlayerState("alice"));
        states.put("bob", new PlayerState("bob"));
        room.setPlayerStates(states);
        return room;
    }
}
