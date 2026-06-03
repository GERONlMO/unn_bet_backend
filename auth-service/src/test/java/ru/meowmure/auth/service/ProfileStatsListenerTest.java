package ru.meowmure.auth.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class ProfileStatsListenerTest {

    @Mock
    private ProfileService profileService;

    @InjectMocks
    private ProfileStatsListener listener;

    @Test
    void handlesProfileMessageOnGameResultsTopic() {
        listener.handleProfileStats("profile:alice,bob|alice|210");

        verify(profileService).recordGameResult("alice,bob", "alice", 210L);
    }

    @Test
    void ignoresWalletMessages() {
        listener.handleProfileStats("alice:210");

        verify(profileService, never()).recordGameResult(org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.anyLong());
    }
}
