package run.esportLab.esportLab.dto;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.Positive;
import lombok.Data;

import java.time.ZonedDateTime;

@Data
public class CreateTeamInviteDto {
    
    @Future(message = "Expiration date must be in the future")
    private ZonedDateTime expiresAt;
    
    @Positive(message = "Max uses must be positive")
    private Integer maxUses; // null = unlimited
    
    // Helper to set expiration in hours from now
    public void setExpiresInHours(int hours) {
        this.expiresAt = ZonedDateTime.now().plusHours(hours);
    }
    
    // Helper to set expiration in days from now
    public void setExpiresInDays(int days) {
        this.expiresAt = ZonedDateTime.now().plusDays(days);
    }
}