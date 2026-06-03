package ru.meowmure.game.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PlayerStateTest {

    @Test
    void defaultConstructorInitializesDefaults() {
        PlayerState state = new PlayerState();
        assertNotNull(state.getCards());
        assertEquals(0L, state.getCurrentBet());
        assertFalse(state.isFolded());
        assertFalse(state.isActedThisRound());
        assertNull(state.getBestCombination());
        assertFalse(state.isAllIn());
    }

    @Test
    void namedConstructorSetsUsername() {
        PlayerState state = new PlayerState("alice");
        assertEquals("alice", state.getUsername());
    }

    @Test
    void settersUpdateState() {
        PlayerState state = new PlayerState("bob");
        state.setCurrentBet(50L);
        state.setFolded(true);
        state.setActedThisRound(true);
        state.setBestCombination("Pair");
        state.setAllIn(true);
        state.getCards().add(new Card(14, "Hearts"));

        assertEquals(50L, state.getCurrentBet());
        assertTrue(state.isFolded());
        assertTrue(state.isActedThisRound());
        assertEquals("Pair", state.getBestCombination());
        assertTrue(state.isAllIn());
        assertEquals(1, state.getCards().size());
    }
}
