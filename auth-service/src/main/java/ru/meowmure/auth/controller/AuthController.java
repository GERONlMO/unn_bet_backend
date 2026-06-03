package ru.meowmure.auth.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import ru.meowmure.auth.dto.AuthRequest;
import ru.meowmure.auth.dto.PublicKeyResponse;
import ru.meowmure.auth.dto.RegisterRequest;
import ru.meowmure.auth.service.AuthCryptoService;
import ru.meowmure.auth.service.AuthService;
import ru.meowmure.auth.validation.AuthInputValidator;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Autowired
    private AuthService service;

    @Autowired
    private AuthCryptoService authCryptoService;

    @Autowired
    private AuthenticationManager authenticationManager;

    @GetMapping("/public-key")
    public PublicKeyResponse getPublicKey() {
        return authCryptoService.getPublicKeyResponse();
    }

    @PostMapping("/register")
    public String addNewUser(@RequestBody RegisterRequest request) {
        service.registerUser(request);
        return "user added to the system";
    }

    @PostMapping("/login")
    public String getToken(@RequestBody AuthRequest authRequest) {
        String username = AuthInputValidator.validateAndNormalizeUsername(
                authCryptoService.decrypt(authRequest.getEncryptedUsername())
        );
        String password = authCryptoService.decrypt(authRequest.getEncryptedPassword());
        AuthInputValidator.validatePasswordForLogin(password);

        Authentication authenticate = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(username, password)
        );
        if (authenticate.isAuthenticated()) {
            return service.generateToken(username);
        }
        throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid access");
    }

    @GetMapping("/validate")
    public String validateToken(@RequestParam("token") String token) {
        if (token == null || token.isBlank() || token.length() > 4096) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid token");
        }
        service.validateToken(token.trim());
        return "Token is valid";
    }

    @PostMapping("/exit")
    public String exit(@RequestHeader(value = "loggedInUser", required = false) String loggedInUser) {
        return "Logged out successfully";
    }
}
