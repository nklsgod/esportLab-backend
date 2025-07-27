package run.esportLab.esportLab.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import run.esportLab.esportLab.model.Member;

import javax.crypto.SecretKey;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;

@Component
@Slf4j
public class JwtUtil {

    private final SecretKey secretKey;
    private final long tokenValidityInHours;

    public JwtUtil(
        @Value("${app.jwt.secret:mySecretKey1234567890123456789012345678901234567890}") String secret,
        @Value("${app.jwt.expiration-hours:24}") long tokenValidityInHours
    ) {
        this.secretKey = Keys.hmacShaKeyFor(secret.getBytes());
        this.tokenValidityInHours = tokenValidityInHours;
    }

    public String generateToken(Member member) {
        Instant now = Instant.now();
        Instant expiration = now.plus(tokenValidityInHours, ChronoUnit.HOURS);

        return Jwts.builder()
            .subject(member.getDiscordUserId())
            .claim("memberId", member.getId())
            .claim("displayName", member.getDisplayName())
            .claim("roles", member.getRoles().toString())
            .issuedAt(Date.from(now))
            .expiration(Date.from(expiration))
            .signWith(secretKey)
            .compact();
    }

    public Claims extractClaims(String token) {
        try {
            return Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
        } catch (Exception e) {
            log.debug("Failed to parse JWT token: {}", e.getMessage());
            return null;
        }
    }

    public String extractDiscordUserId(String token) {
        Claims claims = extractClaims(token);
        return claims != null ? claims.getSubject() : null;
    }

    public Long extractMemberId(String token) {
        Claims claims = extractClaims(token);
        return claims != null ? claims.get("memberId", Long.class) : null;
    }

    public boolean isTokenValid(String token) {
        try {
            Claims claims = extractClaims(token);
            if (claims == null) {
                return false;
            }
            return claims.getExpiration().after(new Date());
        } catch (Exception e) {
            log.debug("Token validation failed: {}", e.getMessage());
            return false;
        }
    }
}