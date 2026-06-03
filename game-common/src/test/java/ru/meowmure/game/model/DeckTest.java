package ru.meowmure.game.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class DeckTest {

    @Test
    void initializeStandardDeckCreates52Cards() {
        Deck deck = new Deck();
        deck.initializeStandardDeck();
        assertEquals(52, deck.getCards().size());
    }

    @Test
    void drawRemovesCardFromDeck() {
        Deck deck = new Deck();
        deck.initializeStandardDeck();
        Card drawn = deck.draw();
        assertNotNull(drawn);
        assertEquals(51, deck.getCards().size());
    }

    @Test
    void drawFromEmptyDeckReturnsNull() {
        Deck deck = new Deck();
        assertNull(deck.draw());
    }

    @Test
    void shuffleKeepsCardCount() {
        Deck deck = new Deck();
        deck.initializeStandardDeck();
        deck.shuffle();
        assertEquals(52, deck.getCards().size());
    }

    @Test
    void addCardIncreasesSize() {
        Deck deck = new Deck();
        deck.addCard(new Card(14, "Hearts"));
        assertEquals(1, deck.getCards().size());
    }
}
