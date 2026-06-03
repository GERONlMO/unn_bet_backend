package ru.meowmure.auth.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.meowmure.auth.dto.ProfileResponse;
import ru.meowmure.auth.entity.UserCredential;
import ru.meowmure.auth.entity.UserRole;
import ru.meowmure.auth.repository.UserRepository;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProfileServiceBatchTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private ProfileService profileService;

    @Test
    void getProfilesByUsernamesReturnsMap() {
        UserCredential user = new UserCredential();
        user.setUsername("alice");
        user.setRole(UserRole.USER);
        user.setAvatar("avatar-data");
        user.setGamesPlayed(5);
        user.setWins(2);
        user.setBestWin(100L);

        when(userRepository.findByUsernameIn(List.of("alice"))).thenReturn(List.of(user));

        Map<String, ProfileResponse> profiles = profileService.getProfilesByUsernames(List.of("alice"));

        assertEquals(1, profiles.size());
        assertEquals("avatar-data", profiles.get("alice").getAvatar());
        assertEquals(5, profiles.get("alice").getGamesPlayed());
    }
}
