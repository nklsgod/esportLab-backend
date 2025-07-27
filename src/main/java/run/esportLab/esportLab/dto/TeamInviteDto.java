package run.esportLab.esportLab.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Builder;
import lombok.Data;

import java.time.ZonedDateTime;

@Data
@Builder
public class TeamInviteDto {
    
    private Long id;
    private String inviteCode;
    private Long teamId;
    private String teamName;
    private String createdByDisplayName;
    
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSXXX")
    private ZonedDateTime expiresAt;
    
    private Integer maxUses;
    private Integer usedCount;
    private Boolean isActive;
    
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSXXX")
    private ZonedDateTime createdAt;
    
    // Computed fields
    private Boolean isExpired;
    private Boolean isValid;
    private Integer remainingUses;
}