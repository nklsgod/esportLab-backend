package run.esportLab.esportLab.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import run.esportLab.esportLab.dto.AvailabilityDto;
import run.esportLab.esportLab.dto.CreateAvailabilityDto;
import run.esportLab.esportLab.dto.TeamAvailabilityOverviewDto;
import run.esportLab.esportLab.service.AvailabilityService;
import run.esportLab.esportLab.service.CustomOAuth2User;

import java.time.ZonedDateTime;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@Slf4j
public class AvailabilityController {
    
    private final AvailabilityService availabilityService;
    
    @GetMapping("/teams/{teamId}/availability")
    public ResponseEntity<TeamAvailabilityOverviewDto> getTeamAvailability(
            @PathVariable Long teamId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) ZonedDateTime from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) ZonedDateTime to,
            @AuthenticationPrincipal CustomOAuth2User currentUser) {
        
        log.debug("Getting availability for team {} from {} to {}", teamId, from, to);
        
        // Verify user has access to this team
        if (currentUser.getMember().getTeam() == null || 
            !currentUser.getMember().getTeam().getId().equals(teamId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        
        TeamAvailabilityOverviewDto overview = availabilityService.getTeamAvailabilityOverview(teamId, from, to);
        return ResponseEntity.ok(overview);
    }
    
    @PostMapping("/availability")
    public ResponseEntity<AvailabilityDto> createAvailability(
            @Valid @RequestBody CreateAvailabilityDto dto,
            @AuthenticationPrincipal CustomOAuth2User currentUser) {
        
        log.debug("Creating availability for user {}", currentUser.getDiscordUserId());
        
        try {
            AvailabilityDto created = availabilityService.createAvailability(
                    currentUser.getMember().getId(), dto);
            return ResponseEntity.ok(created);
        } catch (IllegalArgumentException e) {
            log.warn("Invalid availability request: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }
    
    @PutMapping("/availability/{id}")
    public ResponseEntity<AvailabilityDto> updateAvailability(
            @PathVariable Long id,
            @Valid @RequestBody CreateAvailabilityDto dto,
            @AuthenticationPrincipal CustomOAuth2User currentUser) {
        
        log.debug("Updating availability {} for user {}", id, currentUser.getDiscordUserId());
        
        try {
            AvailabilityDto updated = availabilityService.updateAvailability(id, dto);
            
            // Verify user owns this availability
            if (!updated.getMemberId().equals(currentUser.getMember().getId())) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }
            
            return ResponseEntity.ok(updated);
        } catch (IllegalArgumentException e) {
            log.warn("Invalid availability update request: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }
    
    @DeleteMapping("/availability/{id}")
    public ResponseEntity<Void> deleteAvailability(
            @PathVariable Long id,
            @AuthenticationPrincipal CustomOAuth2User currentUser) {
        
        log.debug("Deleting availability {} for user {}", id, currentUser.getDiscordUserId());
        
        try {
            availabilityService.deleteAvailability(id, currentUser.getMember().getId());
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException e) {
            log.warn("Availability not found: {}", e.getMessage());
            return ResponseEntity.notFound().build();
        } catch (SecurityException e) {
            log.warn("Unauthorized delete attempt: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
    }
}