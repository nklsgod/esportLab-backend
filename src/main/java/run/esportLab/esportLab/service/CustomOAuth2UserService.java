package run.esportLab.esportLab.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import run.esportLab.esportLab.entity.Member;
import run.esportLab.esportLab.repository.MemberRepository;

import java.time.ZonedDateTime;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

    private final MemberRepository memberRepository;

    @Override
    @Transactional
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2User oauth2User = super.loadUser(userRequest);
        
        try {
            return processOAuth2User(oauth2User);
        } catch (Exception ex) {
            log.error("Error processing OAuth2 user", ex);
            throw new OAuth2AuthenticationException("Authentication failed");
        }
    }

    private OAuth2User processOAuth2User(OAuth2User oauth2User) {
        String discordUserId = oauth2User.getAttribute("id");
        String username = oauth2User.getAttribute("username");
        String discriminator = oauth2User.getAttribute("discriminator");
        String avatar = oauth2User.getAttribute("avatar");
        String email = oauth2User.getAttribute("email");

        if (discordUserId == null) {
            throw new OAuth2AuthenticationException("Discord user ID not found");
        }

        // Build display name and avatar URL
        String displayName = discriminator != null && !"0".equals(discriminator) 
            ? username + "#" + discriminator 
            : username;
        
        String avatarUrl = null;
        if (avatar != null) {
            avatarUrl = String.format("https://cdn.discordapp.com/avatars/%s/%s.png", discordUserId, avatar);
        }

        // Find or create member
        Optional<Member> existingMember = memberRepository.findByDiscordUserId(discordUserId);
        Member member;

        if (existingMember.isPresent()) {
            // Update existing member
            member = existingMember.get();
            member.setDisplayName(displayName);
            member.setAvatarUrl(avatarUrl);
            log.info("Updated existing member: {}", discordUserId);
        } else {
            // Create new member
            member = Member.builder()
                .discordUserId(discordUserId)
                .displayName(displayName)
                .avatarUrl(avatarUrl)
                .tz("Europe/Berlin") // Default timezone
                .roles("USER") // Default role
                .createdAt(ZonedDateTime.now())
                .build();
            log.info("Created new member: {}", discordUserId);
        }

        member = memberRepository.save(member);

        // Return custom OAuth2User implementation
        return new CustomOAuth2User(oauth2User, member);
    }
}