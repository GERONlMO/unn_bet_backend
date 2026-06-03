package ru.meowmure.auth.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import ru.meowmure.auth.dto.RegisterRequest;
import ru.meowmure.auth.entity.UserCredential;
import ru.meowmure.auth.entity.UserRole;
import ru.meowmure.auth.repository.UserRepository;
import ru.meowmure.auth.validation.AuthInputValidator;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class AuthService {

    @Value("${app.admin-usernames:}")
    private String adminUsernames;

    @Autowired
    private UserRepository repository;
    @Autowired
    private PasswordEncoder passwordEncoder;
    @Autowired
    private JwtService jwtService;
    @Autowired
    private AuthCryptoService authCryptoService;

    public void registerUser(RegisterRequest request) {
        String username = AuthInputValidator.validateAndNormalizeUsername(
                authCryptoService.decrypt(request.getEncryptedUsername())
        );
        String password = authCryptoService.decrypt(request.getEncryptedPassword());
        AuthInputValidator.validatePasswordForRegistration(password);

        String email = null;
        if (request.getEncryptedEmail() != null && !request.getEncryptedEmail().isBlank()) {
            email = AuthInputValidator.validateAndNormalizeEmail(
                    authCryptoService.decrypt(request.getEncryptedEmail())
            );
        }

        if (repository.findByUsername(username).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Username already exists");
        }

        UserCredential credential = new UserCredential();
        credential.setUsername(username);
        credential.setPassword(passwordEncoder.encode(password));
        credential.setEmail(email);
        credential.setGamesPlayed(0);
        credential.setWins(0);
        credential.setBestWin(0L);
        credential.setRole(resolveRoleForUsername(username));
        repository.save(credential);
    }

    public String generateToken(String username) {
        UserCredential user = repository.findByUsername(username)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
        UserRole expectedRole = resolveRoleForUsername(username);
        if (user.getRole() == null) {
            user.setRole(UserRole.USER);
        }
        if (user.getRole() != expectedRole) {
            user.setRole(expectedRole);
            repository.save(user);
        }
        return jwtService.generateToken(username, user.getRole().name());
    }

    private UserRole resolveRoleForUsername(String username) {
        return configuredAdminUsernames().contains(username) ? UserRole.ADMIN : UserRole.USER;
    }

    private Set<String> configuredAdminUsernames() {
        if (adminUsernames == null || adminUsernames.isBlank()) {
            return Set.of();
        }
        return Arrays.stream(adminUsernames.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toSet());
    }

    public void validateToken(String token) {
        jwtService.validateToken(token);
    }
}
