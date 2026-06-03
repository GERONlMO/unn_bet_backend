package ru.meowmure.auth.validation;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.Base64;

import static org.junit.jupiter.api.Assertions.*;

class AvatarValidatorTest {

    @Test
    void acceptsRawBase64() {
        String png = Base64.getEncoder().encodeToString(new byte[]{1, 2, 3});
        String result = AvatarValidator.normalizeAndValidate(png);
        assertEquals(png, result);
    }

    @Test
    void acceptsDataUrl() {
        String payload = Base64.getEncoder().encodeToString(new byte[]{1, 2, 3});
        String dataUrl = "data:image/png;base64," + payload;
        assertEquals(dataUrl, AvatarValidator.normalizeAndValidate(dataUrl));
    }

    @Test
    void rejectsInvalidBase64() {
        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> AvatarValidator.normalizeAndValidate("not-base64!!!"));
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
    }

    @Test
    void emptyStringBecomesNull() {
        assertNull(AvatarValidator.normalizeAndValidate("   "));
    }
}
