package run.esportLab.esportLab.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class JoinTeamDto {
    
    @NotBlank(message = "Invite code is required")
    @Size(min = 6, max = 16, message = "Invite code must be between 6 and 16 characters")
    private String inviteCode;
}