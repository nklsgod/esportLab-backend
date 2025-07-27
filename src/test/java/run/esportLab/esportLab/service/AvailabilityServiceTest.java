package run.esportLab.esportLab.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import run.esportLab.esportLab.dto.CreateAvailabilityDto;
import run.esportLab.esportLab.dto.TeamAvailabilityOverviewDto;
import run.esportLab.esportLab.entity.Availability;
import run.esportLab.esportLab.entity.Member;
import run.esportLab.esportLab.entity.Team;
import run.esportLab.esportLab.repository.AvailabilityRepository;
import run.esportLab.esportLab.repository.MemberRepository;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AvailabilityServiceTest {

    @Mock
    private AvailabilityRepository availabilityRepository;
    
    @Mock
    private MemberRepository memberRepository;
    
    @InjectMocks
    private AvailabilityService availabilityService;
    
    private Team testTeam;
    private Member testMember;
    private ZonedDateTime baseTime;
    
    @BeforeEach
    void setUp() {
        testTeam = Team.builder()
                .id(1L)
                .name("Test Team")
                .build();
        
        testMember = Member.builder()
                .id(1L)
                .team(testTeam)
                .discordUserId("123456789")
                .displayName("TestUser")
                .roles("USER")
                .build();
        
        // Use a fixed time for predictable tests
        baseTime = ZonedDateTime.of(2025, 3, 15, 10, 0, 0, 0, ZoneId.of("UTC"));
    }
    
    @Test
    @DisplayName("Should create availability successfully")
    void shouldCreateAvailabilitySuccessfully() {
        // Given
        CreateAvailabilityDto dto = new CreateAvailabilityDto();
        dto.setStartsAt(baseTime);
        dto.setEndsAt(baseTime.plusHours(2));
        dto.setAvailable(true);
        dto.setNote("Test availability");
        dto.setTimezone("Europe/Berlin");
        
        when(memberRepository.findById(1L)).thenReturn(Optional.of(testMember));
        when(availabilityRepository.findOverlappingForMember(anyLong(), any(), any()))
                .thenReturn(List.of());
        
        Availability savedAvailability = Availability.builder()
                .id(1L)
                .member(testMember)
                .startsAtUtc(baseTime)
                .endsAtUtc(baseTime.plusHours(2))
                .available(true)
                .note("Test availability")
                .build();
        
        when(availabilityRepository.save(any(Availability.class))).thenReturn(savedAvailability);
        
        // When
        var result = availabilityService.createAvailability(1L, dto);
        
        // Then
        assertThat(result).isNotNull();
        assertThat(result.getMemberId()).isEqualTo(1L);
        assertThat(result.getAvailable()).isTrue();
        assertThat(result.getNote()).isEqualTo("Test availability");
        
        verify(availabilityRepository).save(any(Availability.class));
    }
    
    @Test
    @DisplayName("Should reject overlapping availability")
    void shouldRejectOverlappingAvailability() {
        // Given
        CreateAvailabilityDto dto = new CreateAvailabilityDto();
        dto.setStartsAt(baseTime);
        dto.setEndsAt(baseTime.plusHours(2));
        dto.setAvailable(true);
        dto.setTimezone("Europe/Berlin");
        
        when(memberRepository.findById(1L)).thenReturn(Optional.of(testMember));
        
        Availability existingAvailability = Availability.builder()
                .id(2L)
                .member(testMember)
                .startsAtUtc(baseTime.minusMinutes(30))
                .endsAtUtc(baseTime.plusMinutes(30))
                .available(true)
                .build();
        
        when(availabilityRepository.findOverlappingForMember(anyLong(), any(), any()))
                .thenReturn(List.of(existingAvailability));
        
        // When & Then
        assertThatThrownBy(() -> availabilityService.createAvailability(1L, dto))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("overlaps");
        
        verify(availabilityRepository, never()).save(any());
    }
    
    @Test
    @DisplayName("Should reject availability longer than 24 hours")
    void shouldRejectLongAvailability() {
        // Given
        CreateAvailabilityDto dto = new CreateAvailabilityDto();
        dto.setStartsAt(baseTime);
        dto.setEndsAt(baseTime.plusHours(25)); // More than 24 hours
        dto.setAvailable(true);
        dto.setTimezone("Europe/Berlin");
        
        when(memberRepository.findById(1L)).thenReturn(Optional.of(testMember));
        
        // When & Then
        assertThatThrownBy(() -> availabilityService.createAvailability(1L, dto))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("24 hours");
        
        verify(availabilityRepository, never()).save(any());
    }
    
    @Test
    @DisplayName("Should reject availability with start time after end time")
    void shouldRejectInvalidTimeRange() {
        // Given
        CreateAvailabilityDto dto = new CreateAvailabilityDto();
        dto.setStartsAt(baseTime.plusHours(2));
        dto.setEndsAt(baseTime); // End before start
        dto.setAvailable(true);
        dto.setTimezone("Europe/Berlin");
        
        when(memberRepository.findById(1L)).thenReturn(Optional.of(testMember));
        
        // When & Then
        assertThatThrownBy(() -> availabilityService.createAvailability(1L, dto))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("before end time");
        
        verify(availabilityRepository, never()).save(any());
    }
    
    @Test
    @DisplayName("Should get team availability overview")
    void shouldGetTeamAvailabilityOverview() {
        // Given
        ZonedDateTime from = baseTime;
        ZonedDateTime to = baseTime.plusDays(7);
        
        Availability availability1 = Availability.builder()
                .id(1L)
                .member(testMember)
                .startsAtUtc(baseTime)
                .endsAtUtc(baseTime.plusHours(2))
                .available(true)
                .note("Available")
                .build();
        
        Availability availability2 = Availability.builder()
                .id(2L)
                .member(testMember)
                .startsAtUtc(baseTime.plusDays(1))
                .endsAtUtc(baseTime.plusDays(1).plusHours(3))
                .available(false)
                .note("Unavailable")
                .build();
        
        when(availabilityRepository.findByTeamIdAndTimeRange(1L, from, to))
                .thenReturn(List.of(availability1, availability2));
        
        // When
        TeamAvailabilityOverviewDto result = availabilityService.getTeamAvailabilityOverview(1L, from, to);
        
        // Then
        assertThat(result).isNotNull();
        assertThat(result.getTeamId()).isEqualTo(1L);
        assertThat(result.getMembers()).hasSize(1);
        
        var memberDto = result.getMembers().get(0);
        assertThat(memberDto.getMemberId()).isEqualTo(1L);
        assertThat(memberDto.getAvailabilities()).hasSize(2);
        
        var stats = memberDto.getStats();
        assertThat(stats.getTotalAvailableMinutes()).isEqualTo(120); // 2 hours
        assertThat(stats.getTotalUnavailableMinutes()).isEqualTo(180); // 3 hours
        assertThat(stats.getAvailableSlots()).isEqualTo(1);
        assertThat(stats.getUnavailableSlots()).isEqualTo(1);
    }
    
    @Test
    @DisplayName("DST: Should handle spring forward transition correctly")
    void shouldHandleSpringForwardDSTTransition() {
        // Given: Europe/Berlin spring forward on 2025-03-30 at 02:00 -> 03:00
        ZoneId berlinZone = ZoneId.of("Europe/Berlin");
        
        // Time before DST change (01:30 CET = 00:30 UTC)
        ZonedDateTime berlinTimeBefore = ZonedDateTime.of(2025, 3, 30, 1, 30, 0, 0, berlinZone);
        // Time after DST change (03:30 CEST = 01:30 UTC)
        ZonedDateTime berlinTimeAfter = ZonedDateTime.of(2025, 3, 30, 3, 30, 0, 0, berlinZone);
        
        CreateAvailabilityDto dto = new CreateAvailabilityDto();
        dto.setStartsAt(berlinTimeBefore);
        dto.setEndsAt(berlinTimeAfter);
        dto.setAvailable(true);
        dto.setTimezone("Europe/Berlin");
        
        when(memberRepository.findById(1L)).thenReturn(Optional.of(testMember));
        when(availabilityRepository.findOverlappingForMember(anyLong(), any(), any()))
                .thenReturn(List.of());
        
        // Capture the saved availability to check UTC conversion
        when(availabilityRepository.save(any(Availability.class))).thenAnswer(invocation -> {
            Availability saved = invocation.getArgument(0);
            saved.setId(1L);
            
            // Verify UTC times are correct
            ZonedDateTime expectedStartUtc = ZonedDateTime.of(2025, 3, 30, 0, 30, 0, 0, ZoneId.of("UTC"));
            ZonedDateTime expectedEndUtc = ZonedDateTime.of(2025, 3, 30, 1, 30, 0, 0, ZoneId.of("UTC"));
            
            assertThat(saved.getStartsAtUtc()).isEqualTo(expectedStartUtc);
            assertThat(saved.getEndsAtUtc()).isEqualTo(expectedEndUtc);
            
            // Duration should be 1 hour (because of DST spring forward)
            assertThat(saved.getDurationMinutes()).isEqualTo(60);
            
            return saved;
        });
        
        // When
        var result = availabilityService.createAvailability(1L, dto);
        
        // Then
        assertThat(result).isNotNull();
        assertThat(result.getDurationMinutes()).isEqualTo(60);
        
        verify(availabilityRepository).save(any(Availability.class));
    }
    
    @Test
    @DisplayName("DST: Should handle fall back transition correctly")
    void shouldHandleFallBackDSTTransition() {
        // Given: Simple test with known DST fall back scenario
        ZoneId berlinZone = ZoneId.of("Europe/Berlin");
        
        // Use a simple 2-hour period during fall back (when clocks go back)
        ZonedDateTime berlinStart = ZonedDateTime.of(2025, 10, 26, 1, 0, 0, 0, berlinZone);
        ZonedDateTime berlinEnd = ZonedDateTime.of(2025, 10, 26, 4, 0, 0, 0, berlinZone);
        
        CreateAvailabilityDto dto = new CreateAvailabilityDto();
        dto.setStartsAt(berlinStart);
        dto.setEndsAt(berlinEnd);
        dto.setAvailable(true);
        dto.setTimezone("Europe/Berlin");
        
        when(memberRepository.findById(1L)).thenReturn(Optional.of(testMember));
        when(availabilityRepository.findOverlappingForMember(anyLong(), any(), any()))
                .thenReturn(List.of());
        
        Availability savedAvailability = Availability.builder()
                .id(1L)
                .member(testMember)
                .startsAtUtc(berlinStart.withZoneSameInstant(ZoneId.of("UTC")))
                .endsAtUtc(berlinEnd.withZoneSameInstant(ZoneId.of("UTC")))
                .available(true)
                .build();
        
        when(availabilityRepository.save(any(Availability.class))).thenReturn(savedAvailability);
        
        // When
        var result = availabilityService.createAvailability(1L, dto);
        
        // Then
        assertThat(result).isNotNull();
        assertThat(result.getAvailable()).isTrue();
        verify(availabilityRepository).save(any(Availability.class));
    }
    
    @Test
    @DisplayName("Should allow creating unavailable slots")
    void shouldAllowUnavailableSlots() {
        // Given
        CreateAvailabilityDto dto = new CreateAvailabilityDto();
        dto.setStartsAt(baseTime);
        dto.setEndsAt(baseTime.plusHours(2));
        dto.setAvailable(false); // Explicitly unavailable
        dto.setNote("Cannot attend");
        dto.setTimezone("Europe/Berlin");
        
        when(memberRepository.findById(1L)).thenReturn(Optional.of(testMember));
        when(availabilityRepository.findOverlappingForMember(anyLong(), any(), any()))
                .thenReturn(List.of());
        
        Availability savedAvailability = Availability.builder()
                .id(1L)
                .member(testMember)
                .startsAtUtc(baseTime)
                .endsAtUtc(baseTime.plusHours(2))
                .available(false)
                .note("Cannot attend")
                .build();
        
        when(availabilityRepository.save(any(Availability.class))).thenReturn(savedAvailability);
        
        // When
        var result = availabilityService.createAvailability(1L, dto);
        
        // Then
        assertThat(result).isNotNull();
        assertThat(result.getAvailable()).isFalse();
        assertThat(result.getNote()).isEqualTo("Cannot attend");
        
        verify(availabilityRepository).save(any(Availability.class));
    }
}