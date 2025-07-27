package run.esportLab.esportLab.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import run.esportLab.esportLab.dto.TeamDto;
import run.esportLab.esportLab.dto.UserProfileDto;
import run.esportLab.esportLab.entity.Member;

import java.util.Collections;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class UserService {

    public UserProfileDto getUserProfile(Member member) {
        // Get team IDs - currently supporting single team, but prepared for multi-team
        List<Long> teamIds = member.getTeam() != null 
            ? List.of(member.getTeam().getId())
            : Collections.emptyList();

        // Build team information if member has a team
        TeamDto teamDto = null;
        boolean hasTeam = member.getTeam() != null;
        boolean isTeamOwner = hasTeam && member.hasRole("OWNER");
        boolean isTeamAdmin = hasTeam && member.isAdmin();
        
        if (hasTeam) {
            teamDto = TeamDto.builder()
                    .id(member.getTeam().getId())
                    .name(member.getTeam().getName())
                    .discordGuildId(member.getTeam().getDiscordGuildId())
                    .reminderChannelId(member.getTeam().getReminderChannelId())
                    .timezone(member.getTeam().getTz())
                    .minPlayers(member.getTeam().getMinPlayers())
                    .minDurationMinutes(member.getTeam().getMinDurationMinutes())
                    .reminderHours(member.getTeam().getReminderHoursList())
                    .createdAt(member.getTeam().getCreatedAt())
                    .memberCount(member.getTeam().getMembers().size())
                    .isCurrentUserOwner(isTeamOwner)
                    .isCurrentUserAdmin(isTeamAdmin)
                    .build();
        }

        return UserProfileDto.builder()
            .id(member.getId())
            .discordUserId(member.getDiscordUserId())
            .displayName(member.getDisplayName())
            .avatarUrl(member.getAvatarUrl())
            .tz(member.getTz())
            .roles(member.getRoles())
            .teamIds(teamIds)
            .team(teamDto)
            .hasTeam(hasTeam)
            .isTeamOwner(isTeamOwner)
            .isTeamAdmin(isTeamAdmin)
            .build();
    }
}