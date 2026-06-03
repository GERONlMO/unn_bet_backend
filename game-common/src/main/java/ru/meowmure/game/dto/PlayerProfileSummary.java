package ru.meowmure.game.dto;

/**
 * Публичные данные игрока для лобби и стола (не хранится в Redis).
 */
public class PlayerProfileSummary {

    private String username;
    private String avatar;
    private Integer gamesPlayed;
    private Integer wins;
    private Long bestWin;
    private String role;

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getAvatar() {
        return avatar;
    }

    public void setAvatar(String avatar) {
        this.avatar = avatar;
    }

    public Integer getGamesPlayed() {
        return gamesPlayed;
    }

    public void setGamesPlayed(Integer gamesPlayed) {
        this.gamesPlayed = gamesPlayed;
    }

    public Integer getWins() {
        return wins;
    }

    public void setWins(Integer wins) {
        this.wins = wins;
    }

    public Long getBestWin() {
        return bestWin;
    }

    public void setBestWin(Long bestWin) {
        this.bestWin = bestWin;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }
}
