package ru.meowmure.game.util;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import ru.meowmure.game.model.Card;
import ru.meowmure.game.model.Deck;
import ru.meowmure.game.model.GameRoom;
import ru.meowmure.game.model.PlayerState;
import ru.meowmure.game.support.TestFixtures;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class GameRoomSnapshotLoggerTest {

    private ListAppender<ILoggingEvent> appender;
    private Logger logger;

    @BeforeEach
    void setUp() {
        logger = (Logger) LoggerFactory.getLogger("test");
        appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);
    }

    @Test
    void logSnapshotWithNullRoom() {
        GameRoomSnapshotLogger.logSnapshot(logger, "EVENT", null);
        assertTrue(appender.list.get(0).getFormattedMessage().contains("room=null"));
    }

    @Test
    void logSnapshotIncludesRoomDetails() {
        GameRoom room = TestFixtures.inProgressTwoPlayerRoom("r1");
        room.setWinner("alice");
        room.setWinningCombination("Pair");

        GameRoomSnapshotLogger.logSnapshot(logger, "TEST", room);

        String message = appender.list.get(0).getFormattedMessage();
        assertTrue(message.contains("[ROOM r1] TEST"));
        assertTrue(message.contains("status=IN_PROGRESS"));
        assertTrue(message.contains("player=alice"));
        assertTrue(message.contains("winner=alice"));
    }

    @Test
    void logSnapshotFormatsCardsAndHiddenValues() {
        GameRoom room = new GameRoom("r2", "X", false, "", 6, 10L, 30000L);
        Deck table = new Deck();
        table.setCards(List.of(new Card(-1, "empty"), new Card(14, "Hearts")));
        room.setTableDeck(table);
        PlayerState ps = new PlayerState("u");
        ps.getCards().add(new Card(11, "Clubs"));
        room.setPlayerStates(java.util.Map.of("u", ps));

        GameRoomSnapshotLogger.logSnapshot(logger, "CARDS", room);

        String message = appender.list.get(0).getFormattedMessage();
        assertTrue(message.contains("??"));
        assertTrue(message.contains("AH"));
        assertTrue(message.contains("JC"));
    }
}
