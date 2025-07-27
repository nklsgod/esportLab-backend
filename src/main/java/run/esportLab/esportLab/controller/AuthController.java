package run.esportLab.esportLab.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@Slf4j
public class AuthController {

    @GetMapping("/discord/login")
    public ResponseEntity<Map<String, String>> discordLogin() {
        // This endpoint triggers the OAuth2 flow
        // Spring Security will redirect to Discord automatically
        return ResponseEntity.ok(Map.of(
            "message", "Redirecting to Discord OAuth2...",
            "redirectUrl", "/oauth2/authorization/discord"
        ));
    }

    @PostMapping("/logout")
    public ResponseEntity<Map<String, String>> logout(HttpServletRequest request, 
                                                     HttpServletResponse response) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null) {
            new SecurityContextLogoutHandler().logout(request, response, auth);
            log.info("User logged out: {}", auth.getName());
        }
        
        return ResponseEntity.ok(Map.of("message", "Logged out successfully"));
    }

    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> authStatus(Authentication authentication) {
        if (authentication != null && authentication.isAuthenticated()) {
            return ResponseEntity.ok(Map.of(
                "authenticated", true,
                "principal", authentication.getName()
            ));
        }
        
        return ResponseEntity.ok(Map.of("authenticated", false));
    }
}