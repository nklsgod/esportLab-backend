package run.esportLab.esportLab.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import run.esportLab.esportLab.dto.*;
import run.esportLab.esportLab.entity.Availability;
import run.esportLab.esportLab.entity.Member;
import run.esportLab.esportLab.repository.AvailabilityRepository;
import run.esportLab.esportLab.repository.MemberRepository;

import java.time.Duration;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AvailabilityService {
    
    private final AvailabilityRepository availabilityRepository;
    private final MemberRepository memberRepository;
    
    @Transactional(readOnly = true)
    public TeamAvailabilityOverviewDto getTeamAvailabilityOverview(Long teamId, ZonedDateTime from, ZonedDateTime to) {
        log.debug("Getting availability overview for team {} from {} to {}", teamId, from, to);
        
        // Get all availabilities for the team in the time range
        List<Availability> availabilities = availabilityRepository.findByTeamIdAndTimeRange(teamId, from, to);
        
        // Group by member
        Map<Member, List<Availability>> availabilitiesByMember = availabilities.stream()
                .collect(Collectors.groupingBy(Availability::getMember));
        
        // Convert to DTOs
        List<TeamAvailabilityOverviewDto.MemberAvailabilityDto> memberDtos = availabilitiesByMember.entrySet().stream()
                .map(entry -> {
                    Member member = entry.getKey();
                    List<Availability> memberAvailabilities = entry.getValue();
                    
                    List<AvailabilityDto> availabilityDtos = memberAvailabilities.stream()
                            .map(this::mapToDto)
                            .collect(Collectors.toList());
                    
                    TeamAvailabilityOverviewDto.AvailabilityStatsDto stats = calculateStats(memberAvailabilities);
                    
                    return TeamAvailabilityOverviewDto.MemberAvailabilityDto.builder()
                            .memberId(member.getId())
                            .displayName(member.getDisplayName())
                            .avatarUrl(member.getAvatarUrl())
                            .availabilities(availabilityDtos)
                            .stats(stats)
                            .build();
                })
                .collect(Collectors.toList());
        
        return TeamAvailabilityOverviewDto.builder()
                .teamId(teamId)
                .fromDate(from.toString())
                .toDate(to.toString())
                .members(memberDtos)
                .build();
    }
    
    @Transactional
    public AvailabilityDto createAvailability(Long memberId, CreateAvailabilityDto dto) {
        log.debug("Creating availability for member {} from {} to {}", memberId, dto.getStartsAt(), dto.getEndsAt());
        
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new IllegalArgumentException("Member not found: " + memberId));
        
        // Convert input timezone to UTC
        ZonedDateTime startsAtUtc = convertToUtc(dto.getStartsAt(), dto.getTimezone());
        ZonedDateTime endsAtUtc = convertToUtc(dto.getEndsAt(), dto.getTimezone());
        
        // Validate business rules
        validateAvailabilityRequest(memberId, startsAtUtc, endsAtUtc, null);
        
        Availability availability = Availability.builder()
                .member(member)
                .startsAtUtc(startsAtUtc)
                .endsAtUtc(endsAtUtc)
                .available(dto.getAvailable())
                .note(dto.getNote())
                .build();
        
        availability = availabilityRepository.save(availability);
        log.info("Created availability {} for member {}", availability.getId(), memberId);
        
        return mapToDto(availability);
    }
    
    @Transactional
    public AvailabilityDto updateAvailability(Long availabilityId, CreateAvailabilityDto dto) {
        log.debug("Updating availability {}", availabilityId);
        
        Availability availability = availabilityRepository.findById(availabilityId)
                .orElseThrow(() -> new IllegalArgumentException("Availability not found: " + availabilityId));
        
        // Convert input timezone to UTC
        ZonedDateTime startsAtUtc = convertToUtc(dto.getStartsAt(), dto.getTimezone());
        ZonedDateTime endsAtUtc = convertToUtc(dto.getEndsAt(), dto.getTimezone());
        
        // Validate business rules (exclude current availability from overlap check)
        validateAvailabilityRequest(availability.getMember().getId(), startsAtUtc, endsAtUtc, availabilityId);
        
        availability.setStartsAtUtc(startsAtUtc);
        availability.setEndsAtUtc(endsAtUtc);
        availability.setAvailable(dto.getAvailable());
        availability.setNote(dto.getNote());
        
        availability = availabilityRepository.save(availability);
        log.info("Updated availability {}", availabilityId);
        
        return mapToDto(availability);
    }
    
    @Transactional
    public void deleteAvailability(Long availabilityId, Long requestingMemberId) {
        log.debug("Deleting availability {}", availabilityId);
        
        Availability availability = availabilityRepository.findById(availabilityId)
                .orElseThrow(() -> new IllegalArgumentException("Availability not found: " + availabilityId));
        
        // Check if requesting member is owner or admin
        if (!availability.getMember().getId().equals(requestingMemberId) && 
            !isTeamAdmin(requestingMemberId, availability.getMember().getTeam().getId())) {
            throw new SecurityException("Not authorized to delete this availability");
        }
        
        availabilityRepository.delete(availability);
        log.info("Deleted availability {}", availabilityId);
    }
    
    private void validateAvailabilityRequest(Long memberId, ZonedDateTime startsAtUtc, ZonedDateTime endsAtUtc, Long excludeId) {
        // Validate time range
        if (startsAtUtc.isAfter(endsAtUtc) || startsAtUtc.isEqual(endsAtUtc)) {
            throw new IllegalArgumentException("Start time must be before end time");
        }
        
        // Validate max duration (24 hours)
        Duration duration = Duration.between(startsAtUtc, endsAtUtc);
        if (duration.toHours() > 24) {
            throw new IllegalArgumentException("Availability duration cannot exceed 24 hours");
        }
        
        // Check for overlaps
        List<Availability> overlapping;
        if (excludeId != null) {
            overlapping = availabilityRepository.findOverlappingForMember(memberId, startsAtUtc, endsAtUtc, excludeId);
        } else {
            overlapping = availabilityRepository.findOverlappingForMember(memberId, startsAtUtc, endsAtUtc);
        }
        
        if (!overlapping.isEmpty()) {
            throw new IllegalArgumentException("Availability overlaps with existing entries");
        }
    }
    
    private ZonedDateTime convertToUtc(ZonedDateTime dateTime, String timezone) {
        if (dateTime.getZone().equals(ZoneId.of("UTC"))) {
            return dateTime;
        }
        
        // If input has different timezone, convert to UTC
        ZoneId inputZone = ZoneId.of(timezone);
        return dateTime.withZoneSameInstant(ZoneId.of("UTC"));
    }
    
    private boolean isTeamAdmin(Long memberId, Long teamId) {
        return memberRepository.findById(memberId)
                .map(member -> member.getTeam() != null && 
                              member.getTeam().getId().equals(teamId) && 
                              member.getRoles() != null && 
                              member.getRoles().contains("ADMIN"))
                .orElse(false);
    }
    
    private AvailabilityDto mapToDto(Availability availability) {
        return AvailabilityDto.builder()
                .id(availability.getId())
                .memberId(availability.getMember().getId())
                .memberDisplayName(availability.getMember().getDisplayName())
                .memberAvatarUrl(availability.getMember().getAvatarUrl())
                .startsAt(availability.getStartsAtUtc())
                .endsAt(availability.getEndsAtUtc())
                .available(availability.getAvailable())
                .note(availability.getNote())
                .createdAt(availability.getCreatedAt())
                .build();
    }
    
    private TeamAvailabilityOverviewDto.AvailabilityStatsDto calculateStats(List<Availability> availabilities) {
        long totalAvailableMinutes = 0;
        long totalUnavailableMinutes = 0;
        int availableSlots = 0;
        int unavailableSlots = 0;
        
        for (Availability availability : availabilities) {
            long duration = availability.getDurationMinutes();
            if (availability.isAvailable()) {
                totalAvailableMinutes += duration;
                availableSlots++;
            } else {
                totalUnavailableMinutes += duration;
                unavailableSlots++;
            }
        }
        
        long totalMinutes = totalAvailableMinutes + totalUnavailableMinutes;
        double availabilityPercentage = totalMinutes > 0 ? 
                (double) totalAvailableMinutes / totalMinutes * 100.0 : 0.0;
        
        return TeamAvailabilityOverviewDto.AvailabilityStatsDto.builder()
                .totalAvailableMinutes(totalAvailableMinutes)
                .totalUnavailableMinutes(totalUnavailableMinutes)
                .availableSlots(availableSlots)
                .unavailableSlots(unavailableSlots)
                .availabilityPercentage(Math.round(availabilityPercentage * 100.0) / 100.0)
                .build();
    }
}