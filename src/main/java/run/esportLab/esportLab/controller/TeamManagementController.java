package run.esportLab.esportLab.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import run.esportLab.esportLab.dto.*;
import run.esportLab.esportLab.service.CustomOAuth2User;
import run.esportLab.esportLab.service.TeamManagementService;

import java.util.List;

@RestController
@RequestMapping("/api/teams")
@RequiredArgsConstructor
@Slf4j
public class TeamManagementController {
    
    private final TeamManagementService teamManagementService;
    
    @PostMapping
    public ResponseEntity<TeamDto> createTeam(
            @Valid @RequestBody CreateTeamDto dto,
            @AuthenticationPrincipal CustomOAuth2User currentUser) {
        
        log.debug("Creating team '{}' for user {}", dto.getName(), currentUser.getDiscordUserId());
        
        try {
            TeamDto team = teamManagementService.createTeam(currentUser.getMember().getId(), dto);
            return ResponseEntity.ok(team);
        } catch (IllegalStateException e) {
            log.warn("Team creation failed: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }
    
    @GetMapping("/{teamId}")
    public ResponseEntity<TeamDto> getTeam(
            @PathVariable Long teamId,
            @AuthenticationPrincipal CustomOAuth2User currentUser) {
        
        try {
            TeamDto team = teamManagementService.getTeamInfo(teamId, currentUser.getMember().getId());
            return ResponseEntity.ok(team);
        } catch (SecurityException e) {
            log.warn("Unauthorized team access: {}", e.getMessage());
            return ResponseEntity.status(403).build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }
    
    @PostMapping("/{teamId}/invites")
    public ResponseEntity<TeamInviteDto> createInvite(
            @PathVariable Long teamId,
            @Valid @RequestBody CreateTeamInviteDto dto,
            @AuthenticationPrincipal CustomOAuth2User currentUser) {
        
        log.debug("Creating invite for team {} by user {}", teamId, currentUser.getDiscordUserId());
        
        // Set default expiration if not provided (7 days)
        if (dto.getExpiresAt() == null) {
            dto.setExpiresInDays(7);
        }
        
        try {
            TeamInviteDto invite = teamManagementService.createInvite(
                    teamId, currentUser.getMember().getId(), dto);
            return ResponseEntity.ok(invite);
        } catch (SecurityException e) {
            log.warn("Unauthorized invite creation: {}", e.getMessage());
            return ResponseEntity.status(403).build();
        } catch (IllegalStateException e) {
            log.warn("Invite creation failed: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }
    
    @GetMapping("/{teamId}/invites")
    public ResponseEntity<List<TeamInviteDto>> getTeamInvites(
            @PathVariable Long teamId,
            @AuthenticationPrincipal CustomOAuth2User currentUser) {
        
        try {
            List<TeamInviteDto> invites = teamManagementService.getTeamInvites(
                    teamId, currentUser.getMember().getId());
            return ResponseEntity.ok(invites);
        } catch (SecurityException e) {
            log.warn("Unauthorized invite access: {}", e.getMessage());
            return ResponseEntity.status(403).build();
        }
    }
    
    @DeleteMapping("/{teamId}/invites/{inviteId}")
    public ResponseEntity<Void> deactivateInvite(
            @PathVariable Long teamId,
            @PathVariable Long inviteId,
            @AuthenticationPrincipal CustomOAuth2User currentUser) {
        
        try {
            teamManagementService.deactivateInvite(inviteId, currentUser.getMember().getId());
            return ResponseEntity.noContent().build();
        } catch (SecurityException e) {
            log.warn("Unauthorized invite deactivation: {}", e.getMessage());
            return ResponseEntity.status(403).build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }
    
    @PostMapping("/join")
    public ResponseEntity<TeamDto> joinTeam(
            @Valid @RequestBody JoinTeamDto dto,
            @AuthenticationPrincipal CustomOAuth2User currentUser) {
        
        log.debug("User {} attempting to join team with code {}", 
                currentUser.getDiscordUserId(), dto.getInviteCode());
        
        try {
            TeamDto team = teamManagementService.joinTeam(currentUser.getMember().getId(), dto);
            return ResponseEntity.ok(team);
        } catch (IllegalStateException | IllegalArgumentException e) {
            log.warn("Team join failed: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }
    
    @PostMapping("/leave")
    public ResponseEntity<Void> leaveTeam(
            @AuthenticationPrincipal CustomOAuth2User currentUser) {
        
        log.debug("User {} leaving team", currentUser.getDiscordUserId());
        
        try {
            teamManagementService.leaveTeam(currentUser.getMember().getId());
            return ResponseEntity.noContent().build();
        } catch (IllegalStateException e) {
            log.warn("Team leave failed: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }
}