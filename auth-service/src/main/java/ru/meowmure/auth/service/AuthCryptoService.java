package ru.meowmure.auth.service;

import jakarta.annotation.PostConstruct;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import ru.meowmure.auth.dto.PublicKeyResponse;

import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PublicKey;
import java.security.spec.MGF1ParameterSpec;
import java.util.Base64;
import javax.crypto.Cipher;
import javax.crypto.spec.OAEPParameterSpec;
import javax.crypto.spec.PSource;

@Service
public class AuthCryptoService {

    private static final String TRANSFORMATION = "RSA/ECB/OAEPPadding";
    private static final String ALGORITHM_NAME = "RSA-OAEP-256";
    private static final OAEPParameterSpec OAEP_SHA256 = new OAEPParameterSpec(
            "SHA-256",
            "MGF1",
            MGF1ParameterSpec.SHA256,
            PSource.PSpecified.DEFAULT
    );

    private KeyPair keyPair;

    @PostConstruct
    void initKeyPair() {
        try {
            KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
            generator.initialize(2048);
            this.keyPair = generator.generateKeyPair();
        } catch (Exception e) {
            throw new IllegalStateException("Failed to initialize auth encryption keys", e);
        }
    }

    public PublicKeyResponse getPublicKeyResponse() {
        return new PublicKeyResponse(toPem(keyPair.getPublic()), ALGORITHM_NAME);
    }

    public String decrypt(String encryptedBase64) {
        if (encryptedBase64 == null || encryptedBase64.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Encrypted value is required");
        }
        try {
            byte[] ciphertext = Base64.getDecoder().decode(encryptedBase64.trim());
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.DECRYPT_MODE, keyPair.getPrivate(), OAEP_SHA256);
            byte[] plaintext = cipher.doFinal(ciphertext);
            return new String(plaintext, StandardCharsets.UTF_8);
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid encrypted payload encoding");
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Failed to decrypt credentials");
        }
    }

    String toPem(PublicKey publicKey) {
        String base64 = Base64.getMimeEncoder(64, new byte[]{'\n'})
                .encodeToString(publicKey.getEncoded());
        return "-----BEGIN PUBLIC KEY-----\n" + base64 + "\n-----END PUBLIC KEY-----";
    }

    void setKeyPairForTests(KeyPair keyPair) {
        this.keyPair = keyPair;
    }
}
