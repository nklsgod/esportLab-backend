package run.esportLab.esportLab.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/debug")
@RequiredArgsConstructor
public class DebugController {

    @Value("${DISCORD_CLIENT_ID:NOT_SET}")
    private String clientId;
    
    @Value("${DISCORD_REDIRECT_URI:NOT_SET}")
    private String redirectUri;

    @GetMapping("/oauth2")
    public Map<String, String> getOAuth2Config() {
        return Map.of(
            "clientId", clientId.length() > 8 ? clientId.substring(0, 8) + "..." : clientId,
            "redirectUri", redirectUri
        );
    }
}