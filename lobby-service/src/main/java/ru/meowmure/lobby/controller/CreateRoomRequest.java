package ru.meowmure.lobby.controller;

public class CreateRoomRequest {
    private String name;
    private boolean isPrivate;
    private String password;
    private int maxPlayers;
    private Long minBet;
    private Long turnTimeLimit;

    public Long getTurnTimeLimit() {
        return turnTimeLimit;
    }

    public void setTurnTimeLimit(Long turnTimeLimit) {
        this.turnTimeLimit = turnTimeLimit;
    }

    public int getMaxPlayers() {
        return maxPlayers;
    }

    public void setMaxPlayers(int maxPlayers) {
        this.maxPlayers = maxPlayers;
    }

    public Long getMinBet() {
        return minBet;
    }

    public void setMinBet(Long minBet) {
        this.minBet = minBet;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public boolean isPrivate() {
        return isPrivate;
    }

    public void setPrivate(boolean aPrivate) {
        isPrivate = aPrivate;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}
