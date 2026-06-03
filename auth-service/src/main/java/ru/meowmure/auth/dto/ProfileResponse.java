package ru.meowmure.auth.dto;

import ru.meowmure.auth.entity.UserCredential;
import ru.meowmure.auth.entity.UserRole;

/**
 * Публичные данные профиля для фронта.
 * avatar — base64 или data URL картинки, как прислал фронт.
 */
public class ProfileResponse {

    private String username;
    private Integer gamesPlayed;
    private Integer wins;
    private String avatar;
    private Long bestWin;
    private UserRole role;

    public static ProfileResponse from(UserCredential user) {
        ProfileResponse response = new ProfileResponse();
        response.setUsername(user.getUsername());
        response.setRole(user.getRole() != null ? user.getRole() : UserRole.USER);
        response.setGamesPlayed(user.getGamesPlayed() != null ? user.getGamesPlayed() : 0);
        response.setWins(user.getWins() != null ? user.getWins() : 0);
        response.setAvatar(user.getAvatar());
        response.setBestWin(user.getBestWin() != null ? user.getBestWin() : 0L);
        return response;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
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

    public String getAvatar() {
        return avatar;
    }

    public void setAvatar(String avatar) {
        this.avatar = avatar;
    }

    public Long getBestWin() {
        return bestWin;
    }

    public void setBestWin(Long bestWin) {
        this.bestWin = bestWin;
    }

    public UserRole getRole() {
        return role;
    }

    public void setRole(UserRole role) {
        this.role = role;
    }
}
