package ru.meowmure.game.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class PlayerState implements Serializable {
    private String username;
    private List<Card> cards;
    private Long currentBet;
    private boolean folded;
    private boolean actedThisRound;
    private String bestCombination;
    private boolean isAllIn;

    public PlayerState() {
        this.cards = new ArrayList<>();
        this.currentBet = 0L;
        this.folded = false;
        this.actedThisRound = false;
        this.bestCombination = null;
        this.isAllIn = false;
    }

    public PlayerState(String username) {
        this.username = username;
        this.cards = new ArrayList<>();
        this.currentBet = 0L;
        this.folded = false;
        this.actedThisRound = false;
        this.bestCombination = null;
        this.isAllIn = false;
    }

    public boolean isAllIn() {
        return isAllIn;
    }

    public void setAllIn(boolean allIn) {
        isAllIn = allIn;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public List<Card> getCards() {
        return cards;
    }

    public void setCards(List<Card> cards) {
        this.cards = cards;
    }

    public Long getCurrentBet() {
        return currentBet;
    }

    public void setCurrentBet(Long currentBet) {
        this.currentBet = currentBet;
    }

    public boolean isFolded() {
        return folded;
    }

    public void setFolded(boolean folded) {
        this.folded = folded;
    }

    public boolean isActedThisRound() {
        return actedThisRound;
    }

    public void setActedThisRound(boolean actedThisRound) {
        this.actedThisRound = actedThisRound;
    }

    public String getBestCombination() {
        return bestCombination;
    }

    public void setBestCombination(String bestCombination) {
        this.bestCombination = bestCombination;
    }
}
