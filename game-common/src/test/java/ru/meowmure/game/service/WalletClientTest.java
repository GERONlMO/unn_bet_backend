package ru.meowmure.game.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;
import ru.meowmure.game.support.TestFixtures;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WalletClientTest {

    @Mock
    private RestTemplate restTemplate;

    @InjectMocks
    private WalletClient walletClient;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(walletClient, "walletServiceUrl", "http://wallet:8082");
        ReflectionTestUtils.setField(walletClient, "internalToken", "test-token");
    }

    @Test
    void getBalanceReturnsValue() {
        when(restTemplate.getForEntity(eq("http://wallet:8082/api/wallet/alice"), eq(Map.class)))
                .thenReturn(ResponseEntity.ok(Map.of("balance", 500)));

        assertEquals(500L, walletClient.getBalance("alice"));
    }

    @Test
    void getBalanceReturnsZeroOnFailure() {
        when(restTemplate.getForEntity(anyString(), eq(Map.class)))
                .thenThrow(new RestClientException("down"));

        assertEquals(0L, walletClient.getBalance("alice"));
    }

    @Test
    void getBalanceReturnsZeroWhenBodyMissingBalance() {
        when(restTemplate.getForEntity(anyString(), eq(Map.class)))
                .thenReturn(ResponseEntity.ok(Map.of("username", "alice")));

        assertEquals(0L, walletClient.getBalance("alice"));
    }

    @Test
    void deductBalanceCallsInternalEndpoint() {
        when(restTemplate.exchange(anyString(), eq(HttpMethod.POST), any(HttpEntity.class), eq(Void.class)))
                .thenReturn(ResponseEntity.ok().build());

        walletClient.deductBalance("alice", 10L);

        ArgumentCaptor<String> urlCaptor = ArgumentCaptor.forClass(String.class);
        verify(restTemplate).exchange(urlCaptor.capture(), eq(HttpMethod.POST), any(HttpEntity.class), eq(Void.class));
        assertTrue(urlCaptor.getValue().contains("/internal/wallet/alice/deduct?amount=10"));
    }

    @Test
    void deductBalanceSkipsNonPositiveAmount() {
        walletClient.deductBalance("alice", 0L);
        verify(restTemplate, never()).exchange(anyString(), any(), any(), eq(Void.class));
    }

    @Test
    void deductBalanceThrowsOnFailure() {
        when(restTemplate.exchange(anyString(), eq(HttpMethod.POST), any(HttpEntity.class), eq(Void.class)))
                .thenThrow(new RestClientException("insufficient"));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> walletClient.deductBalance("alice", 10L));
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
    }
}
