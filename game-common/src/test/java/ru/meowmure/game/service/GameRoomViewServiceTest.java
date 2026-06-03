package ru.meowmure.game.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.meowmure.game.dto.PlayerProfileSummary;
import ru.meowmure.game.model.GameRoom;
import ru.meowmure.game.support.TestFixtures;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GameRoomViewServiceTest {

    @Mock
    private WalletClient walletClient;

    @Mock
    private ProfileClient profileClient;

    @InjectMocks
    private GameRoomViewService gameRoomViewService;

    @Test
    void createViewForUserIncludesBalanceAndProfile() {
        GameRoom room = TestFixtures.waitingRoom("r1");
        when(walletClient.getBalance("alice")).thenReturn(750L);
        when(walletClient.getBalance("bob")).thenReturn(500L);
        PlayerProfileSummary aliceProfile = profile("alice", "data:image/png;base64,abc");
        when(profileClient.getProfiles(any())).thenReturn(Map.of("alice", aliceProfile, "bob", profile("bob", null)));

        GameRoom view = gameRoomViewService.createViewForUser(room, "alice");

        assertEquals(750L, view.getViewerBalance());
        assertEquals(750L, view.getPlayerBalances().get("alice"));
        assertEquals("data:image/png;base64,abc", view.getPlayerProfiles().get("alice").getAvatar());
        assertEquals("Test Room", view.getName());
    }

    @Test
    void createViewForUserSkipsBalanceWhenUsernameBlank() {
        GameRoom room = TestFixtures.waitingRoom("r1");
        when(walletClient.getBalance("alice")).thenReturn(750L);
        when(walletClient.getBalance("bob")).thenReturn(500L);
        when(profileClient.getProfiles(any())).thenReturn(Map.of());

        GameRoom view = gameRoomViewService.createViewForUser(room, null);

        assertNull(view.getViewerBalance());
        assertEquals(750L, view.getPlayerBalances().get("alice"));
    }

    @Test
    void createViewsForRoomsSkipsProfiles() {
        GameRoom room = TestFixtures.waitingRoom("r1");
        when(walletClient.getBalance(any())).thenReturn(100L);

        List<GameRoom> views = gameRoomViewService.createViewsForRooms(List.of(room), "alice");

        assertEquals(1, views.size());
        assertTrue(views.get(0).getPlayerProfiles().isEmpty());
        verify(profileClient, never()).getProfiles(any());
    }

    private static PlayerProfileSummary profile(String username, String avatar) {
        PlayerProfileSummary p = new PlayerProfileSummary();
        p.setUsername(username);
        p.setAvatar(avatar);
        p.setGamesPlayed(1);
        p.setWins(0);
        p.setBestWin(0L);
        p.setRole("USER");
        return p;
    }
}
