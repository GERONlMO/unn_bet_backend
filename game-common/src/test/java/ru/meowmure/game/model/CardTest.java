package ru.meowmure.game.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CardTest {

    @Test
    void constructorAndGettersSetters() {
        Card card = new Card(14, "Hearts");
        assertEquals(14, card.getValue());
        assertEquals("Hearts", card.getSuit());

        card.setValue(10);
        card.setSuit("Spades");
        assertEquals(10, card.getValue());
        assertEquals("Spades", card.getSuit());
    }

    @Test
    void defaultConstructor() {
        Card card = new Card();
        assertEquals(0, card.getValue());
        assertNull(card.getSuit());
    }
}
