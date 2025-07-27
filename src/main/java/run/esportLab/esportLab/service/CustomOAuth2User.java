package run.esportLab.esportLab.service;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.core.user.OAuth2User;
import run.esportLab.esportLab.entity.Member;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;

@RequiredArgsConstructor
public class CustomOAuth2User implements OAuth2User {

    private final OAuth2User oauth2User;
    private final Member member;

    @Override
    public Map<String, Object> getAttributes() {
        return oauth2User.getAttributes();
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