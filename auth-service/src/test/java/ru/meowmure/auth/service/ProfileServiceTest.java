package ru.meowmure.auth.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.meowmure.auth.dto.ProfileResponse;
import ru.meowmure.auth.entity.UserCredential;
import ru.meowmure.auth.repository.UserRepository;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProfileServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private ProfileService profileService;

    @Test
    void getProfileReturnsPublicFieldsOnly() {
        UserCredential user = new UserCredential();
        user.setUsername("alice");
        user.setPassword("secret");
        user.setGamesPlayed(3);
        user.setWins(1);
        user.setAvatar("data:image/png;base64,abc=");
        user.setBestWin(250L);
        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(user));

        ProfileResponse profile = profileService.getProfile("alice");

        assertEquals("alice", profile.getUsername());
        assertEquals(3, profile.getGamesPlayed());
        assertEquals(1, profile.getWins());
        assertEquals("data:image/png;base64,abc=", profile.getAvatar());
        assertEquals(250L, profile.getBestWin());
    }

    @Test
    void recordGameResultUpdatesStats() {
        UserCredential alice = new UserCredential();
        alice.setUsername("alice");
        alice.setGamesPlayed(0);
        alice.setWins(0);
        alice.setBestWin(0L);

        UserCredential bob = new UserCredential();
        bob.setUsername("bob");
        bob.setGamesPlayed(1);
        bob.setWins(0);
        bob.setBestWin(100L);

        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(alice));
        when(userRepository.findByUsername("bob")).thenReturn(Optional.of(bob));
        when(userRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        profileService.recordGameResult("alice,bob", "alice", 500L);

        ArgumentCaptor<UserCredential> captor = ArgumentCaptor.forClass(UserCredential.class);
        verify(userRepository, atLeast(2)).save(captor.capture());

        assertEquals(1, alice.getGamesPlayed());
        assertEquals(1, alice.getWins());
        assertEquals(500L, alice.getBestWin());
        assertEquals(2, bob.getGamesPlayed());
    }
}
