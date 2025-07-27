package run.esportLab.esportLab.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import run.esportLab.esportLab.service.CustomOAuth2User;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@Component
@RequiredArgsConstructor
@Slf4j
public class OAuth2AuthenticationSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private final JwtUtil jwtUtil;
    
    @Value("${app.frontend.url:https://esportlab.run}")
    private String frontendUrl;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, 
                                        Authentication authentication) throws IOException {
        
        CustomOAuth2User oauth2User = (CustomOAuth2User) authentication.getPrincipal();
        String token = jwtUtil.generateToken(oauth2User.getMember());
        
        log.info("Generated JWT token for user: {}", oauth2User.getDiscordUserId());
        
        String redirectUrl = frontendUrl + "?token=" + URLEncoder.encode(token, StandardCharsets.UTF_8);
        
        getRedirectStrategy().sendRedirect(request, response, redirectUrl);
    }
}