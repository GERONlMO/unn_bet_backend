package ru.meowmure.auth.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import ru.meowmure.auth.controller.ProfileUpdateRequest;
import ru.meowmure.auth.dto.ProfileResponse;
import ru.meowmure.auth.entity.UserCredential;
import ru.meowmure.auth.repository.UserRepository;
import ru.meowmure.auth.validation.AvatarValidator;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class ProfileService {

    private static final Logger log = LoggerFactory.getLogger(ProfileService.class);

    @Autowired
    private UserRepository userRepository;

    @Transactional(readOnly = true)
    public ProfileResponse getProfile(String username) {
        return ProfileResponse.from(findUser(username));
    }

    @Transactional(readOnly = true)
    public Map<String, ProfileResponse> getProfilesByUsernames(List<String> usernames) {
        if (usernames == null || usernames.isEmpty()) {
            return Map.of();
        }
        List<String> unique = usernames.stream()
                .filter(u -> u != null && !u.isBlank())
                .distinct()
                .toList();
        if (unique.isEmpty()) {
            return Map.of();
        }
        Map<String, ProfileResponse> result = new HashMap<>();
        for (UserCredential user : userRepository.findByUsernameIn(unique)) {
            result.put(user.getUsername(), ProfileResponse.from(user));
        }
        return result;
    }

    @Transactional
    public ProfileResponse updateProfile(String username, ProfileUpdateRequest request) {
        UserCredential user = findUser(username);
        if (request.getAvatar() != null) {
            user.setAvatar(AvatarValidator.normalizeAndValidate(request.getAvatar()));
        }
        if (request.getEmail() != null) {
            user.setEmail(request.getEmail().trim().isEmpty() ? null : request.getEmail().trim());
        }
        return ProfileResponse.from(userRepository.save(user));
    }

    @Transactional
    public void recordGameResult(String playersCsv, String winnersCsv, long winAmountPerWinner) {
        Set<String> players = parseNames(playersCsv);
        Set<String> winners = parseNames(winnersCsv);
        if (players.isEmpty()) {
            return;
        }

        for (String player : players) {
            userRepository.findByUsername(player).ifPresent(user -> {
                user.setGamesPlayed(nullToZero(user.getGamesPlayed()) + 1);
                userRepository.save(user);
            });
        }

        for (String winner : winners) {
            userRepository.findByUsername(winner).ifPresent(user -> {
                user.setWins(nullToZero(user.getWins()) + 1);
                long currentBest = user.getBestWin() != null ? user.getBestWin() : 0L;
                if (winAmountPerWinner > currentBest) {
                    user.setBestWin(winAmountPerWinner);
                }
                userRepository.save(user);
                log.info("[PROFILE STATS] user={} gamesPlayed={} wins={} bestWin={}",
                        user.getUsername(), user.getGamesPlayed(), user.getWins(), user.getBestWin());
            });
        }
    }

    private UserCredential findUser(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
    }

    private static Set<String> parseNames(String csv) {
        if (csv == null || csv.isBlank()) {
            return Set.of();
        }
        return Arrays.stream(csv.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toSet());
    }

    private static int nullToZero(Integer value) {
        return value != null ? value : 0;
    }
}
