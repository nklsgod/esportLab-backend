package run.esportLab.esportLab.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.ZonedDateTime;

@Data
public class CreateAvailabilityDto {
    
    @NotNull(message = "Start time is required")
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSXXX")
    private ZonedDateTime startsAt;
    
    @NotNull(message = "End time is required")
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSXXX")
    private ZonedDateTime endsAt;
    
    @NotNull(message = "Available flag is required")
    private Boolean available;
    
    @Size(max = 500, message = "Note cannot exceed 500 characters")
    private String note;
    
    private String timezone = "Europe/Berlin"; // Default timezone
}