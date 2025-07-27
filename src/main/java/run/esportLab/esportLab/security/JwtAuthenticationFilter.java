package run.esportLab.esportLab.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import run.esportLab.esportLab.entity.Member;
import run.esportLab.esportLab.service.CustomOAuth2User;
import run.esportLab.esportLab.repository.MemberRepository;

import java.io.IOException;
import java.util.Collections;
import java.util.Optional;

@Component
@RequiredArgsConstructor
@Slf4j
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;
    private final MemberRepository memberRepository;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) 
            throws ServletException, IOException {
        
        String authHeader = request.getHeader("Authorization");
        
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        String token = authHeader.substring(7);
        
        if (!jwtUtil.isTokenValid(token)) {
            log.debug("Invalid JWT token");
            filterChain.doFilter(request, response);
            return;
        }

        String discordUserId = jwtUtil.extractDiscordUserId(token);
        Long memberId = jwtUtil.extractMemberId(token);
        
        if (discordUserId != null && memberId != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            try {
                Optional<Member> memberOpt = memberRepository.findById(memberId);
                if (memberOpt.isPresent()) {
                    Member member = memberOpt.get();
                    if (member.getDiscordUserId().equals(discordUserId)) {
                        CustomOAuth2User oauth2User = new CustomOAuth2User(member);
                        
                        UsernamePasswordAuthenticationToken authentication = 
                            new UsernamePasswordAuthenticationToken(oauth2User, null, Collections.emptyList());
                        authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                        
                        SecurityContextHolder.getContext().setAuthentication(authentication);
                        log.debug("Successfully authenticated user: {}", discordUserId);
                    }
                }
            } catch (Exception e) {
                log.debug("Failed to authenticate user from JWT: {}", e.getMessage());
            }
        }

        filterChain.doFilter(request, response);
    }
}