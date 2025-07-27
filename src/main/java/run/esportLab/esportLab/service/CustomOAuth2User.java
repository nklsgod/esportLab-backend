package run.esportLab.esportLab.service;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.core.user.OAuth2User;
import run.esportLab.esportLab.entity.Member;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;

public class CustomOAuth2User implements OAuth2User {

    private final OAuth2User oauth2User;
    private final Member member;
    
    // Constructor for OAuth2 login
    public CustomOAuth2User(OAuth2User oauth2User, Member member) {
        this.oauth2User = oauth2User;
        this.member = member;
    }
    
    // Constructor for JWT authentication (no OAuth2User available)
    public CustomOAuth2User(Member member) {
        this.oauth2User = null;
        this.member = member;
    }

    @Override
    public Map<String, Object> getAttributes() {
        if (oauth2User != null) {
            return oauth2User.getAttributes();
        }
        // Return empty map for JWT authentication
        return Collections.emptyMap();
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        // Parse roles from member entity
        String roles = member.getRoles();
        if (roles == null || roles.isEmpty()) {
            return Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER"));
        }
        
        return Collections.singletonList(new SimpleGrantedAuthority("ROLE_" + roles));
    }

    @Override
    public String getName() {
        return member.getDiscordUserId();
    }

    public Member getMember() {
        return member;
    }

    public String getDiscordUserId() {
        return member.getDiscordUserId();
    }

    public String getDisplayName() {
        return member.getDisplayName();
    }

    public String getAvatarUrl() {
        return member.getAvatarUrl();
    }
}