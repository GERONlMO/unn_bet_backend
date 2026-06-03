package ru.meowmure.game.model;

import org.junit.jupiter.api.Test;
import ru.meowmure.game.support.TestFixtures;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class GameRoomTest {

    @Test
    void getSeatedPlayersOrderReturnsSeatsInOrder() {
        GameRoom room = TestFixtures.seatedRoom("r1");
        assertEquals(List.of("alice", "bob"), room.getSeatedPlayersOrder());
    }

    @Test
    void createViewForUserHidesOpponentCardsDuringGame() {
        GameRoom room = TestFixtures.inProgressTwoPlayerRoom("r1");
        GameRoom aliceView = room.createViewForUser("alice");
        GameRoom bobView = room.createViewForUser("bob");

        assertEquals(14, aliceView.getPlayerStates().get("alice").getCards().get(0).getValue());
        assertEquals(-1, aliceView.getPlayerStates().get("bob").getCards().get(0).getValue());
        assertEquals(2, bobView.getPlayerStates().get("bob").getCards().get(0).getValue());
        assertEquals(-1, bobView.getPlayerStates().get("alice").getCards().get(0).getValue());
    }

    @Test
    void createViewForUserRevealsAllCardsWhenFinished() {
        GameRoom room = TestFixtures.inProgressTwoPlayerRoom("r1");
        room.setStatus("FINISHED");
        room.setCurrentRound(4);

        GameRoom view = room.createViewForUser("alice");
        assertEquals(2, view.getPlayerStates().get("bob").getCards().get(0).getValue());
    }

    @Test
    void createViewForUserMasksTableCardsByRound() {
        GameRoom room = TestFixtures.inProgressTwoPlayerRoom("r1");

        room.setCurrentRound(0);
        assertTrue(room.createViewForUser("alice").getTableDeck().getCards().stream()
                .allMatch(c -> c.getValue() == -1));

        room.setCurrentRound(1);
        long visibleFlop = room.createViewForUser("alice").getTableDeck().getCards().stream()
                .filter(c -> c.getValue() > 0).count();
        assertEquals(3, visibleFlop);

        room.setCurrentRound(2);
        long visibleTurn = room.createViewForUser("alice").getTableDeck().getCards().stream()
                .filter(c -> c.getValue() > 0).count();
        assertEquals(4, visibleTurn);

        room.setCurrentRound(3);
        long visibleRiver = room.createViewForUser("alice").getTableDeck().getCards().stream()
                .filter(c -> c.getValue() > 0).count();
        assertEquals(5, visibleRiver);
    }

    @Test
    void createViewForUserWithoutTableDeck() {
        GameRoom room = TestFixtures.waitingRoom("r1");
        assertNull(room.createViewForUser("alice").getTableDeck());
    }

    @Test
    void createViewForUserHidesReadyStatusDuringGame() {
        GameRoom room = TestFixtures.waitingRoom("r1");
        assertNotNull(room.createViewForUser("alice").getReadyStatus());

        room.setStatus("IN_PROGRESS");
        assertNull(room.createViewForUser("alice").getReadyStatus());

        room.setStatus("FINISHED");
        assertNull(room.createViewForUser("alice").getReadyStatus());
    }

    @Test
    void constructorAndGettersSetters() {
        GameRoom room = new GameRoom("id", "name", true, "pwd", 4, 50L, 15000L);
        assertEquals("id", room.getRoomId());
        assertEquals("name", room.getName());
        assertTrue(room.isPrivate());
        assertEquals("pwd", room.getPassword());
        assertEquals(4, room.getMaxPlayers());
        assertEquals(50L, room.getMinBet());
        assertEquals(15000L, room.getTurnTimeLimit());
        assertEquals("WAITING", room.getStatus());
        assertFalse(room.getRoundTransitionPending());

        room.setViewerBalance(500L);
        assertEquals(500L, room.getViewerBalance());
        room.setPot(100L);
        assertEquals(100L, room.getPot());
    }

    @Test
    void defaultConstructorInitializesCollections() {
        GameRoom room = new GameRoom();
        assertNotNull(room.getPlayers());
        assertNotNull(room.getReadyStatus());
        assertNotNull(room.getPlayerStates());
        assertNotNull(room.getSeats());
    }

    @Test
    void getSeatedPlayersOrderWithEmptySeats() {
        GameRoom room = new GameRoom();
        assertTrue(room.getSeatedPlayersOrder().isEmpty());
    }

    @Test
    void createViewForUserCopiesLobbyFields() {
        GameRoom room = TestFixtures.waitingRoom("r1");
        room.setPrivate(true);
        room.setPassword("secret");
        Map<Integer, String> seats = new HashMap<>();
        seats.put(0, "alice");
        room.setSeats(seats);

        GameRoom view = room.createViewForUser("alice");
        assertTrue(view.isPrivate());
        assertEquals("secret", view.getPassword());
        assertEquals(seats, view.getSeats());
        assertNull(view.getMainDeck());
    }
}
