package ru.meowmure.auth.validation;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.Base64;
import java.util.regex.Pattern;

/**
 * Аватар с фронта: base64 или data URL (data:image/png;base64,...).
 */
public final class AvatarValidator {

    /** ~1.5 МБ исходного файла в base64. */
    public static final int MAX_AVATAR_LENGTH = 2_000_000;

    private static final Pattern DATA_URL_PREFIX = Pattern.compile(
            "^data:image/(jpeg|jpg|png|webp|gif);base64$",
            Pattern.CASE_INSENSITIVE
    );

    private AvatarValidator() {
    }

    public static String normalizeAndValidate(String raw) {
        if (raw == null) {
            return null;
        }
        String avatar = raw.trim();
        if (avatar.isEmpty()) {
            return null;
        }
        if (avatar.length() > MAX_AVATAR_LENGTH) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Avatar image is too large (max ~1.5 MB)");
        }

        String base64Payload = avatar;
        if (avatar.startsWith("data:")) {
            int comma = avatar.indexOf(',');
            if (comma < 0) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid avatar data URL");
            }
            String mimePart = avatar.substring(0, comma);
            if (!DATA_URL_PREFIX.matcher(mimePart).matches()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Avatar must be JPEG, PNG, WebP or GIF");
            }
            base64Payload = avatar.substring(comma + 1);
        }

        try {
            Base64.getDecoder().decode(base64Payload.replaceAll("\\s", ""));
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Avatar is not valid base64");
        }

        return avatar;
    }
}
