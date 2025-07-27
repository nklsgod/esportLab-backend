package run.esportLab.esportLab.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import run.esportLab.esportLab.dto.UserProfileDto;
import run.esportLab.esportLab.service.CustomOAuth2User;
import run.esportLab.esportLab.service.UserService;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@Slf4j
public class UserController {

    private final UserService userService;

    @GetMapping("/me")
    public ResponseEntity<UserProfileDto> getCurrentUser(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(401).build();
        }

        CustomOAuth2User oauth2User = (CustomOAuth2User) authentication.getPrincipal();
        UserProfileDto profile = userService.getUserProfile(oauth2User.getMember());
        
        log.info("Retrieved profile for user: {}", oauth2User.getDiscordUserId());
        return ResponseEntity.ok(profile);
    }
}