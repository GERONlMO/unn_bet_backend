package ru.meowmure.auth.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import ru.meowmure.auth.dto.ProfileResponse;
import ru.meowmure.auth.service.ProfileService;

@RestController
@RequestMapping("/api/profile")
public class ProfileController {

    @Autowired
    private ProfileService profileService;

    @GetMapping("/get")
    public ProfileResponse getProfile(@RequestHeader(value = "loggedInUser", required = false) String loggedInUser) {
        if (loggedInUser == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Not authenticated");
        }
        return profileService.getProfile(loggedInUser);
    }

    @PostMapping("/edit")
    public ProfileResponse editProfile(@RequestHeader(value = "loggedInUser", required = false) String loggedInUser,
                                       @RequestBody ProfileUpdateRequest updateRequest) {
        if (loggedInUser == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Not authenticated");
        }
        return profileService.updateProfile(loggedInUser, updateRequest);
    }
}
