package run.esportLab.esportLab.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserProfileDto {
    private Long id;
    private String discordUserId;
    private String displayName;
    private String avatarUrl;
    private String tz;
    private String roles;
    private List<Long> teamIds;
}