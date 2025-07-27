package run.esportLab.esportLab.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

@Data
public class CreateTeamDto {
    
    @NotBlank(message = "Team name is required")
    @Size(min = 2, max = 50, message = "Team name must be between 2 and 50 characters")
    private String name;
    
    @Size(max = 100, message = "Discord Guild ID cannot exceed 100 characters")
    private String discordGuildId;
    
    @Size(max = 100, message = "Reminder Channel ID cannot exceed 100 characters")
    private String reminderChannelId;
    
    @Size(max = 50, message = "Timezone cannot exceed 50 characters")
    private String timezone = "Europe/Berlin";
    
    @NotNull(message = "Minimum players is required")
    @Positive(message = "Minimum players must be positive")
    private Integer minPlayers = 4;
    
    @NotNull(message = "Minimum duration is required")
    @Positive(message = "Minimum duration must be positive")
    private Integer minDurationMinutes = 90;
    
    private List<Integer> reminderHours = List.of(0, 6, 12, 18);
}