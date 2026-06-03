package ru.meowmure.game.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import ru.meowmure.game.dto.PlayerProfileSummary;
import ru.meowmure.game.model.GameRoom;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class GameRoomViewService {

    @Autowired
    private WalletClient walletClient;

    @Autowired
    private ProfileClient profileClient;

    /**
     * Полная view одной комнаты: балансы + аватарки только участников этой комнаты.
     */
    public GameRoom createViewForUser(GameRoom room, String username) {
        Map<String, PlayerProfileSummary> profiles =
                profileClient.getProfiles(collectParticipantNames(room));
        return buildView(room, username, profiles);
    }

    /**
     * Список комнат в лобби — без playerProfiles (тяжёлые аватарки не нужны на списке).
     */
    public List<GameRoom> createViewsForRooms(List<GameRoom> rooms, String username) {
        if (rooms == null || rooms.isEmpty()) {
            return List.of();
        }
        return rooms.stream()
                .map(room -> buildView(room, username, Collections.emptyMap()))
                .collect(Collectors.toList());
    }

    private GameRoom buildView(GameRoom room, String username, Map<String, PlayerProfileSummary> profiles) {
        GameRoom view = room.createViewForUser(username);
        enrichWithBalances(view);
        enrichWithProfiles(view, profiles);
        if (username != null && !username.isEmpty()) {
            view.setViewerBalance(view.getPlayerBalances().get(username));
        }
        return view;
    }

    private void enrichWithBalances(GameRoom view) {
        Map<String, Long> balances = new HashMap<>();
        for (String player : collectParticipantNames(view)) {
            balances.put(player, walletClient.getBalance(player));
        }
        view.setPlayerBalances(balances);
    }

    private void enrichWithProfiles(GameRoom view, Map<String, PlayerProfileSummary> profiles) {
        if (profiles == null || profiles.isEmpty()) {
            view.setPlayerProfiles(Collections.emptyMap());
            return;
        }
        Map<String, PlayerProfileSummary> roomProfiles = new HashMap<>();
        for (String player : collectParticipantNames(view)) {
            PlayerProfileSummary profile = profiles.get(player);
            if (profile != null) {
                roomProfiles.put(player, profile);
            }
        }
        view.setPlayerProfiles(roomProfiles);
    }

    static Set<String> collectParticipantNames(GameRoom room) {
        Set<String> names = new HashSet<>();
        if (room.getPlayers() != null) {
            room.getPlayers().stream().filter(u -> u != null && !u.isBlank()).forEach(names::add);
        }
        if (room.getSeats() != null) {
            room.getSeats().values().stream().filter(u -> u != null && !u.isBlank()).forEach(names::add);
        }
        if (room.getPlayerStates() != null) {
            room.getPlayerStates().keySet().stream().filter(u -> u != null && !u.isBlank()).forEach(names::add);
        }
        return names;
    }
}
