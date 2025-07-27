package run.esportLab.esportLab.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class TeamAvailabilityOverviewDto {
    
    private Long teamId;
    private String teamName;
    private String fromDate;
    private String toDate;
    private List<MemberAvailabilityDto> members;
    
    @Data
    @Builder
    public static class MemberAvailabilityDto {
        private Long memberId;
        private String displayName;
        private String avatarUrl;
        private List<AvailabilityDto> availabilities;
        private AvailabilityStatsDto stats;
    }
    
    @Data
    @Builder
    public static class AvailabilityStatsDto {
        private long totalAvailableMinutes;
        private long totalUnavailableMinutes;
        private int availableSlots;
        private int unavailableSlots;
        private double availabilityPercentage;
    }
}