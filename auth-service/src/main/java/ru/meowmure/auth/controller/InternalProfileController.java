package ru.meowmure.auth.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.meowmure.auth.dto.ProfileResponse;
import ru.meowmure.auth.service.ProfileService;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/internal/profile")
public class InternalProfileController {

    @Autowired
    private ProfileService profileService;

    @PostMapping("/batch")
    public Map<String, ProfileResponse> getProfilesBatch(@RequestBody List<String> usernames) {
        return profileService.getProfilesByUsernames(usernames);
    }
}
