package ru.meowmure.auth.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import javax.crypto.Cipher;
import javax.crypto.spec.OAEPParameterSpec;
import javax.crypto.spec.PSource;
import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.spec.MGF1ParameterSpec;
import java.util.Base64;

import static org.junit.jupiter.api.Assertions.*;

class AuthCryptoServiceTest {

    private static final OAEPParameterSpec OAEP_SHA256 = new OAEPParameterSpec(
            "SHA-256",
            "MGF1",
            MGF1ParameterSpec.SHA256,
            PSource.PSpecified.DEFAULT
    );

    private AuthCryptoService authCryptoService;
    private KeyPair keyPair;

    @BeforeEach
    void setUp() throws Exception {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048);
        keyPair = generator.generateKeyPair();

        authCryptoService = new AuthCryptoService();
        authCryptoService.setKeyPairForTests(keyPair);
    }

    @Test
    void returnsPublicKeyPem() {
        assertTrue(authCryptoService.getPublicKeyResponse().getPublicKey().contains("BEGIN PUBLIC KEY"));
        assertEquals("RSA-OAEP-256", authCryptoService.getPublicKeyResponse().getAlgorithm());
    }

    @Test
    void decryptsEncryptedPayload() throws Exception {
        String plaintext = "secretPassword123";
        String encrypted = encryptWithPublicKey(plaintext);

        assertEquals(plaintext, authCryptoService.decrypt(encrypted));
    }

    @Test
    void rejectsInvalidBase64() {
        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> authCryptoService.decrypt("not-valid-base64!!!"));
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
    }

    private String encryptWithPublicKey(String plaintext) throws Exception {
        Cipher cipher = Cipher.getInstance("RSA/ECB/OAEPPadding");
        cipher.init(Cipher.ENCRYPT_MODE, keyPair.getPublic(), OAEP_SHA256);
        byte[] encrypted = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));
        return Base64.getEncoder().encodeToString(encrypted);
    }
}
