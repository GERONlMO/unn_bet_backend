package ru.meowmure.game.support;

import ru.meowmure.game.model.Card;
import ru.meowmure.game.model.Deck;
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

    public static GameRoom inProgressTwoPlayerRoom(String roomId) {
        GameRoom room = seatedRoom(roomId);
        room.setStatus("IN_PROGRESS");
        room.setCurrentRound(0);
        room.setPot(15L);
        room.setCurrentHighestBet(10L);
        room.setCurrentTurn("alice");
        room.setTurnStartTime(System.currentTimeMillis());

        PlayerState alice = new PlayerState("alice");
        alice.setCurrentBet(5L);
        alice.setActedThisRound(false);
        alice.getCards().add(new Card(14, "Hearts"));
        alice.getCards().add(new Card(13, "Spades"));

        PlayerState bob = new PlayerState("bob");
        bob.setCurrentBet(10L);
        bob.setActedThisRound(true);
        bob.getCards().add(new Card(2, "Clubs"));
        bob.getCards().add(new Card(3, "Diamonds"));

        Map<String, PlayerState> states = new HashMap<>();
        states.put("alice", alice);
        states.put("bob", bob);
        room.setPlayerStates(states);

        Deck table = new Deck();
        table.setCards(List.of(
                new Card(7, "Hearts"),
                new Card(8, "Hearts"),
                new Card(9, "Hearts"),
                new Card(10, "Hearts"),
                new Card(11, "Hearts")
        ));
        room.setTableDeck(table);
        return room;
    }

    public static GameRoom inProgressThreePlayerRoom(String roomId) {
        GameRoom room = new GameRoom(roomId, "Three", false, "", 6, 10L, 30000L);
        room.setStatus("IN_PROGRESS");
        room.setCurrentRound(0);
        room.setPot(15L);
        room.setCurrentHighestBet(10L);
        room.setCurrentTurn("carol");
        room.setTurnStartTime(System.currentTimeMillis());

        Map<Integer, String> seats = new HashMap<>();
        seats.put(0, "alice");
        seats.put(1, "bob");
        seats.put(2, "carol");
        room.setSeats(seats);
        room.setPlayers(new ArrayList<>(List.of("alice", "bob", "carol")));

        PlayerState alice = player("alice", 5L, false);
        PlayerState bob = player("bob", 10L, true);
        PlayerState carol = player("carol", 0L, false);

        Map<String, PlayerState> states = new HashMap<>();
        states.put("alice", alice);
        states.put("bob", bob);
        states.put("carol", carol);
        room.setPlayerStates(states);
        room.setTableDeck(defaultTableDeck());
        return room;
    }

    private static PlayerState player(String name, long bet, boolean acted) {
        PlayerState ps = new PlayerState(name);
        ps.setCurrentBet(bet);
        ps.setActedThisRound(acted);
        ps.getCards().add(new Card(10, "Hearts"));
        ps.getCards().add(new Card(9, "Spades"));
        return ps;
    }

    private static Deck defaultTableDeck() {
        Deck table = new Deck();
        table.setCards(List.of(
                new Card(2, "Hearts"),
                new Card(4, "Hearts"),
                new Card(6, "Hearts"),
                new Card(8, "Hearts"),
                new Card(10, "Hearts")
        ));
        return table;
    }
}
