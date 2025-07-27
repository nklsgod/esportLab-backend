package run.esportLab.esportLab.dto;

import lombok.Builder;
import lombok.Data;

import java.time.ZonedDateTime;
import java.util.List;

@Data
@Builder
public class TeamDto {
    
    private Long id;
    private String name;
    private String discordGuildId;
    private String reminderChannelId;
    private String timezone;
    private Integer minPlayers;
    private Integer minDurationMinutes;
    private List<Integer> reminderHours;
    private ZonedDateTime createdAt;
    
    private Integer memberCount;
    private String ownerDisplayName;
    private Boolean isCurrentUserOwner;
    private Boolean isCurrentUserAdmin;
}