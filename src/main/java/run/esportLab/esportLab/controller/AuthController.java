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
    public void discordLogin(HttpServletResponse response) throws Exception {
        // Direct redirect to Discord OAuth2 authorization
        response.sendRedirect("/oauth2/authorization/discord");
    }

    // Logout is handled by Spring Security automatically at /auth/logout

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
    
    @PostMapping("/logout")
    public ResponseEntity<Map<String, String>> logout() {
        // With JWT, logout is handled client-side by removing the token
        return ResponseEntity.ok(Map.of("message", "Logged out successfully"));
    }
}