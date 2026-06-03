package ru.meowmure.game.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ru.meowmure.game.model.Card;
import ru.meowmure.game.model.PlayerState;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class CombinationObserverTest {

    private CombinationObserver observer;

    @BeforeEach
    void setUp() {
        observer = new CombinationObserver();
    }

    @Test
    void evaluateAllHandsEmptyInput() {
        assertTrue(observer.evaluateAllHands(null, List.of()).isEmpty());
        assertTrue(observer.evaluateAllHands(new ArrayList<>(), List.of()).isEmpty());
    }

    @Test
    void getWinnersEmptyInput() {
        assertTrue(observer.getWinners(new ArrayList<>()).isEmpty());
    }

    @Test
    void evaluateBestHandReturnsNullWhenEmpty() {
        assertNull(observer.evaluateBestHand(new ArrayList<>(), List.of()));
    }

    @Test
    void detectsPair() {
        PlayerState player = new PlayerState("alice");
        player.getCards().add(new Card(14, "Hearts"));
        player.getCards().add(new Card(14, "Spades"));
        List<Card> table = List.of(
                new Card(2, "Clubs"), new Card(5, "Diamonds"), new Card(7, "Hearts"),
                new Card(9, "Spades"), new Card(11, "Clubs")
        );

        CombinationObserver.HandResult result = observer.evaluateBestHand(List.of(player), table);
        assertEquals("Pair", result.combinationName);
        assertEquals(2, result.rank);
    }

    @Test
    void detectsTwoPair() {
        PlayerState player = new PlayerState("alice");
        player.getCards().add(new Card(14, "Hearts"));
        player.getCards().add(new Card(13, "Spades"));
        List<Card> table = List.of(
                new Card(14, "Clubs"), new Card(13, "Diamonds"), new Card(7, "Hearts"),
                new Card(9, "Spades"), new Card(2, "Clubs")
        );

        CombinationObserver.HandResult result = observer.evaluateBestHand(List.of(player), table);
        assertEquals("Two Pair", result.combinationName);
        assertEquals(3, result.rank);
    }

    @Test
    void detectsThreeOfAKind() {
        PlayerState player = new PlayerState("alice");
        player.getCards().add(new Card(14, "Hearts"));
        player.getCards().add(new Card(14, "Spades"));
        List<Card> table = List.of(
                new Card(14, "Clubs"), new Card(5, "Diamonds"), new Card(7, "Hearts"),
                new Card(9, "Spades"), new Card(2, "Clubs")
        );

        CombinationObserver.HandResult result = observer.evaluateBestHand(List.of(player), table);
        assertEquals("Three of a Kind", result.combinationName);
        assertEquals(4, result.rank);
    }

    @Test
    void detectsFourOfAKind() {
        PlayerState player = new PlayerState("alice");
        player.getCards().add(new Card(14, "Hearts"));
        player.getCards().add(new Card(14, "Spades"));
        List<Card> table = List.of(
                new Card(14, "Clubs"), new Card(14, "Diamonds"), new Card(7, "Hearts"),
                new Card(9, "Spades"), new Card(2, "Clubs")
        );

        CombinationObserver.HandResult result = observer.evaluateBestHand(List.of(player), table);
        assertEquals("Four of a Kind", result.combinationName);
        assertEquals(8, result.rank);
    }

    @Test
    void detectsHighCard() {
        PlayerState player = new PlayerState("alice");
        player.getCards().add(new Card(14, "Hearts"));
        player.getCards().add(new Card(13, "Spades"));
        List<Card> table = List.of(
                new Card(2, "Clubs"), new Card(5, "Diamonds"), new Card(7, "Hearts"),
                new Card(9, "Spades"), new Card(11, "Clubs")
        );

        CombinationObserver.HandResult result = observer.evaluateBestHand(List.of(player), table);
        assertEquals("High Card", result.combinationName);
        assertEquals(1, result.rank);
    }

    @Test
    void getWinnersReturnsSingleWinner() {
        PlayerState p1 = new PlayerState("alice");
        p1.getCards().add(new Card(14, "Hearts"));
        p1.getCards().add(new Card(14, "Spades"));
        PlayerState p2 = new PlayerState("bob");
        p2.getCards().add(new Card(2, "Clubs"));
        p2.getCards().add(new Card(3, "Diamonds"));
        List<Card> table = List.of(
                new Card(14, "Clubs"), new Card(5, "Diamonds"), new Card(7, "Hearts"),
                new Card(9, "Spades"), new Card(11, "Clubs")
        );

        List<CombinationObserver.HandResult> sorted = observer.evaluateAllHands(List.of(p1, p2), table);
        List<CombinationObserver.HandResult> winners = observer.getWinners(sorted);
        assertEquals(1, winners.size());
        assertEquals("alice", winners.get(0).username);
    }

    @Test
    void getWinnersReturnsSplitPotForTie() {
        PlayerState p1 = new PlayerState("alice");
        p1.getCards().add(new Card(10, "Hearts"));
        p1.getCards().add(new Card(9, "Spades"));
        PlayerState p2 = new PlayerState("bob");
        p2.getCards().add(new Card(10, "Clubs"));
        p2.getCards().add(new Card(9, "Diamonds"));
        List<Card> table = List.of(
                new Card(2, "Clubs"), new Card(5, "Diamonds"), new Card(7, "Hearts"),
                new Card(11, "Spades"), new Card(13, "Clubs")
        );

        List<CombinationObserver.HandResult> sorted = observer.evaluateAllHands(List.of(p1, p2), table);
        List<CombinationObserver.HandResult> winners = observer.getWinners(sorted);
        assertEquals(2, winners.size());
    }

    @Test
    void handResultConstructorStoresFields() {
        CombinationObserver.HandResult hr = new CombinationObserver.HandResult("u", 5, "Straight", List.of(10, 9));
        assertEquals("u", hr.username);
        assertEquals(5, hr.rank);
        assertEquals("Straight", hr.combinationName);
        assertEquals(List.of(10, 9), hr.tieBreakers);
    }

    @Test
    void evaluateAllHandsSortsByRank() {
        PlayerState strong = new PlayerState("alice");
        strong.getCards().add(new Card(14, "Hearts"));
        strong.getCards().add(new Card(14, "Spades"));
        PlayerState weak = new PlayerState("bob");
        weak.getCards().add(new Card(2, "Clubs"));
        weak.getCards().add(new Card(3, "Diamonds"));
        List<Card> table = List.of(
                new Card(14, "Clubs"), new Card(5, "Diamonds"), new Card(7, "Hearts"),
                new Card(9, "Spades"), new Card(11, "Clubs")
        );

        List<CombinationObserver.HandResult> results = observer.evaluateAllHands(List.of(weak, strong), table);
        assertEquals("alice", results.get(0).username);
    }
}
