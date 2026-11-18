package com.tlcn.backend.user.controller;

import com.tlcn.backend.auth.service.LoginTokenService;
import com.tlcn.backend.user.dto.*;
import com.tlcn.backend.user.service.ProfileService;
import jakarta.servlet.http.*;
import jakarta.validation.Valid;
import org.springframework.http.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/users/me")
public class ProfileController {
    private final ProfileService profiles;
    private final LoginTokenService tokens;

    public ProfileController(ProfileService profiles, LoginTokenService tokens) {
        this.profiles = profiles;
        this.tokens = tokens;
    }

    @GetMapping
    public ProfileResponse get(@AuthenticationPrincipal UserSummary actor) {
        return profiles.getProfile(actor.id());
    }

    @PutMapping
    public ProfileResponse update(@AuthenticationPrincipal UserSummary actor,
            @Valid @RequestBody UpdateProfileRequest body) {
        return profiles.updateProfile(actor.id(), body);
    }

    @PostMapping(value = "/avatar", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ProfileResponse avatar(@AuthenticationPrincipal UserSummary actor, @RequestPart("file") MultipartFile file) {
        return profiles.updateAvatar(actor.id(), file);
    }

    @PutMapping("/password")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void password(@AuthenticationPrincipal UserSummary actor, @Valid @RequestBody ChangePasswordRequest body,
            HttpServletRequest request, HttpServletResponse response) {
        profiles.changePassword(actor.id(), body);
        tokens.logout(request, response);
    }
}
