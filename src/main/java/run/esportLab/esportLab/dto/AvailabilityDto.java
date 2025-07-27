package run.esportLab.esportLab.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Builder;
import lombok.Data;

import java.time.ZonedDateTime;

@Data
@Builder
public class AvailabilityDto {
    
    private Long id;
    private Long memberId;
    private String memberDisplayName;
    private String memberAvatarUrl;
    
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSXXX")
    private ZonedDateTime startsAt;
    
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSXXX")
    private ZonedDateTime endsAt;
    
    private Boolean available;
    private String note;
    
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSXXX")
    private ZonedDateTime createdAt;
    
    public long getDurationMinutes() {
        if (startsAt == null || endsAt == null) {
            return 0;
        }
        return java.time.Duration.between(startsAt, endsAt).toMinutes();
    }
}