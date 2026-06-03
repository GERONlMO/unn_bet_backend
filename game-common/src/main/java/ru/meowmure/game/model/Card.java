package ru.meowmure.game.model;

import java.io.Serializable;

public class Card implements Serializable {
    private int value; // 2-14 (11=J, 12=Q, 13=K, 14=A)
    private String suit; // Hearts, Diamonds, Clubs, Spades

    public Card() {}

    public Card(int value, String suit) {
        this.value = value;
        this.suit = suit;
    }

    public int getValue() {
        return value;
    }

    public void setValue(int value) {
        this.value = value;
    }

    public String getSuit() {
        return suit;
    }

    public void setSuit(String suit) {
        this.suit = suit;
    }
}
