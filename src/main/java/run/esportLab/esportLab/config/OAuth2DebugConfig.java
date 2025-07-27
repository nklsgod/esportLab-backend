package run.esportLab.esportLab.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;

@Configuration
@Slf4j
public class OAuth2DebugConfig {

    @Value("${DISCORD_CLIENT_ID:NOT_SET}")
    private String clientId;
    
    @Value("${DISCORD_CLIENT_SECRET:NOT_SET}")
    private String clientSecret;
    
    @Value("${DISCORD_REDIRECT_URI:NOT_SET}")
    private String redirectUri;

    @EventListener
    public void handleContextRefresh(ContextRefreshedEvent event) {
        log.info("=== OAuth2 Configuration Debug ===");
        log.info("Discord Client ID: {}", clientId.substring(0, Math.min(clientId.length(), 8)) + "...");
        log.info("Discord Client Secret: {}", clientSecret.equals("NOT_SET") ? "NOT_SET" : "SET");
        log.info("Discord Redirect URI: {}", redirectUri);
        log.info("=== End OAuth2 Configuration Debug ===");
    }
}