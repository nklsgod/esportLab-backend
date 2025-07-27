package run.esportLab.esportLab.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
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

        return UserProfileDto.builder()
            .id(member.getId())
            .discordUserId(member.getDiscordUserId())
            .displayName(member.getDisplayName())
            .avatarUrl(member.getAvatarUrl())
            .tz(member.getTz())
            .roles(member.getRoles())
            .teamIds(teamIds)
            .build();
    }
}