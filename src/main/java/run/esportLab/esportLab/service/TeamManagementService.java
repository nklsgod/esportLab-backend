package run.esportLab.esportLab.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import run.esportLab.esportLab.dto.*;
import run.esportLab.esportLab.entity.Member;
import run.esportLab.esportLab.entity.Team;
import run.esportLab.esportLab.entity.TeamInvite;
import run.esportLab.esportLab.repository.MemberRepository;
import run.esportLab.esportLab.repository.TeamInviteRepository;
import run.esportLab.esportLab.repository.TeamRepository;

import java.security.SecureRandom;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class TeamManagementService {
    
    private final TeamRepository teamRepository;
    private final MemberRepository memberRepository;
    private final TeamInviteRepository teamInviteRepository;
    
    private static final String INVITE_CODE_CHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
    private static final int INVITE_CODE_LENGTH = 8;
    private static final SecureRandom random = new SecureRandom();
    
    @Transactional
    public TeamDto createTeam(Long creatorMemberId, CreateTeamDto dto) {
        log.debug("Creating team '{}' for member {}", dto.getName(), creatorMemberId);
        
        Member creator = memberRepository.findById(creatorMemberId)
                .orElseThrow(() -> new IllegalArgumentException("Member not found: " + creatorMemberId));
        
        if (creator.getTeam() != null) {
            throw new IllegalStateException("Member is already in a team. Leave current team first.");
        }
        
        Team team = Team.builder()
                .name(dto.getName())
                .discordGuildId(dto.getDiscordGuildId())
                .reminderChannelId(dto.getReminderChannelId())
                .tz(dto.getTimezone())
                .minPlayers(dto.getMinPlayers())
                .minDurationMinutes(dto.getMinDurationMinutes())
                .build();
        
        if (dto.getReminderHours() != null && !dto.getReminderHours().isEmpty()) {
            team.setReminderHoursList(dto.getReminderHours());
        }
        
        team = teamRepository.save(team);
        
        // Make creator the owner
        creator.setTeam(team);
        creator.setRoles("OWNER,ADMIN,PLAYER");
        memberRepository.save(creator);
        
        log.info("Created team {} with owner {}", team.getId(), creatorMemberId);
        
        return mapToDto(team, creator);
    }
    
    @Transactional
    public TeamInviteDto createInvite(Long teamId, Long creatorMemberId, CreateTeamInviteDto dto) {
        log.debug("Creating invite for team {} by member {}", teamId, creatorMemberId);
        
        Member creator = memberRepository.findById(creatorMemberId)
                .orElseThrow(() -> new IllegalArgumentException("Member not found: " + creatorMemberId));
        
        if (creator.getTeam() == null || !creator.getTeam().getId().equals(teamId)) {
            throw new SecurityException("Member is not part of this team");
        }
        
        if (!creator.isAdmin()) {
            throw new SecurityException("Only admins can create invites");
        }
        
        // Check invite limit (max 5 active invites per team)
        long activeInvites = teamInviteRepository.countActiveInvitesByTeamId(teamId);
        if (activeInvites >= 5) {
            throw new IllegalStateException("Maximum number of active invites reached (5)");
        }
        
        String inviteCode = generateUniqueInviteCode();
        
        TeamInvite invite = TeamInvite.builder()
                .team(creator.getTeam())
                .inviteCode(inviteCode)
                .createdBy(creator)
                .expiresAt(dto.getExpiresAt())
                .maxUses(dto.getMaxUses())
                .build();
        
        invite = teamInviteRepository.save(invite);
        
        log.info("Created invite {} for team {} (expires: {})", inviteCode, teamId, dto.getExpiresAt());
        
        return mapToDto(invite);
    }
    
    @Transactional
    public TeamDto joinTeam(Long memberId, JoinTeamDto dto) {
        log.debug("Member {} attempting to join team with code {}", memberId, dto.getInviteCode());
        
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new IllegalArgumentException("Member not found: " + memberId));
        
        if (member.getTeam() != null) {
            throw new IllegalStateException("Member is already in a team. Leave current team first.");
        }
        
        TeamInvite invite = teamInviteRepository.findValidInviteByCode(dto.getInviteCode(), ZonedDateTime.now())
                .orElseThrow(() -> new IllegalArgumentException("Invalid or expired invite code"));
        
        // Join the team
        member.setTeam(invite.getTeam());
        member.setRoles("PLAYER");
        member = memberRepository.save(member);
        
        // Update invite usage
        invite.incrementUsage();
        teamInviteRepository.save(invite);
        
        log.info("Member {} joined team {} using invite {}", memberId, invite.getTeam().getId(), dto.getInviteCode());
        
        return mapToDto(invite.getTeam(), member);
    }
    
    @Transactional
    public void leaveTeam(Long memberId) {
        log.debug("Member {} leaving team", memberId);
        
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new IllegalArgumentException("Member not found: " + memberId));
        
        if (member.getTeam() == null) {
            throw new IllegalStateException("Member is not in a team");
        }
        
        Team team = member.getTeam();
        Long teamId = team.getId();
        
        // Check if member is the only owner
        List<Member> owners = memberRepository.findByTeamIdAndRolesContaining(teamId, "OWNER");
        if (owners.size() == 1 && owners.get(0).getId().equals(memberId)) {
            // Transfer ownership to another admin, or delete team if no other admins
            List<Member> admins = memberRepository.findByTeamIdAndRolesContaining(teamId, "ADMIN")
                    .stream()
                    .filter(m -> !m.getId().equals(memberId))
                    .toList();
            
            if (admins.isEmpty()) {
                // No other admins, delete the team
                log.info("Deleting team {} as last owner is leaving", teamId);
                deleteTeam(teamId);
                return;
            } else {
                // Transfer ownership to first admin
                Member newOwner = admins.get(0);
                newOwner.setRoles(newOwner.getRoles() + ",OWNER");
                memberRepository.save(newOwner);
                log.info("Transferred ownership of team {} from {} to {}", teamId, memberId, newOwner.getId());
            }
        }
        
        // Remove member from team
        member.setTeam(null);
        member.setRoles("USER");
        memberRepository.save(member);
        
        log.info("Member {} left team {}", memberId, teamId);
    }
    
    @Transactional
    public void deleteTeam(Long teamId) {
        log.debug("Deleting team {}", teamId);
        
        // Deactivate all invites
        teamInviteRepository.deactivateAllInvitesForTeam(teamId);
        
        // Remove all members from team
        List<Member> members = memberRepository.findByTeamId(teamId);
        for (Member member : members) {
            member.setTeam(null);
            member.setRoles("USER");
        }
        memberRepository.saveAll(members);
        
        // Delete the team (cascade will handle training sessions, etc.)
        teamRepository.deleteById(teamId);
        
        log.info("Deleted team {} and removed {} members", teamId, members.size());
    }
    
    @Transactional
    public void deactivateInvite(Long inviteId, Long memberId) {
        log.debug("Deactivating invite {} by member {}", inviteId, memberId);
        
        TeamInvite invite = teamInviteRepository.findById(inviteId)
                .orElseThrow(() -> new IllegalArgumentException("Invite not found: " + inviteId));
        
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new IllegalArgumentException("Member not found: " + memberId));
        
        if (!member.getTeam().getId().equals(invite.getTeam().getId()) || !member.isAdmin()) {
            throw new SecurityException("Only team admins can deactivate invites");
        }
        
        invite.deactivate();
        teamInviteRepository.save(invite);
        
        log.info("Deactivated invite {} by member {}", inviteId, memberId);
    }
    
    public List<TeamInviteDto> getTeamInvites(Long teamId, Long memberId) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new IllegalArgumentException("Member not found: " + memberId));
        
        if (!member.getTeam().getId().equals(teamId) || !member.isAdmin()) {
            throw new SecurityException("Only team admins can view invites");
        }
        
        return teamInviteRepository.findActiveInvitesByTeamId(teamId)
                .stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }
    
    public TeamDto getTeamInfo(Long teamId, Long memberId) {
        Team team = teamRepository.findById(teamId)
                .orElseThrow(() -> new IllegalArgumentException("Team not found: " + teamId));
        
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new IllegalArgumentException("Member not found: " + memberId));
        
        if (!member.getTeam().getId().equals(teamId)) {
            throw new SecurityException("Member is not part of this team");
        }
        
        return mapToDto(team, member);
    }
    
    private String generateUniqueInviteCode() {
        String code;
        do {
            code = generateInviteCode();
        } while (teamInviteRepository.existsByInviteCode(code));
        return code;
    }
    
    private String generateInviteCode() {
        StringBuilder code = new StringBuilder(INVITE_CODE_LENGTH);
        for (int i = 0; i < INVITE_CODE_LENGTH; i++) {
            code.append(INVITE_CODE_CHARS.charAt(random.nextInt(INVITE_CODE_CHARS.length())));
        }
        return code.toString();
    }
    
    private TeamDto mapToDto(Team team, Member currentMember) {
        List<Member> owners = memberRepository.findByTeamIdAndRolesContaining(team.getId(), "OWNER");
        String ownerDisplayName = owners.isEmpty() ? "Unknown" : owners.get(0).getDisplayName();
        
        return TeamDto.builder()
                .id(team.getId())
                .name(team.getName())
                .discordGuildId(team.getDiscordGuildId())
                .reminderChannelId(team.getReminderChannelId())
                .timezone(team.getTz())
                .minPlayers(team.getMinPlayers())
                .minDurationMinutes(team.getMinDurationMinutes())
                .reminderHours(team.getReminderHoursList())
                .createdAt(team.getCreatedAt())
                .memberCount(team.getMembers().size())
                .ownerDisplayName(ownerDisplayName)
                .isCurrentUserOwner(currentMember.hasRole("OWNER"))
                .isCurrentUserAdmin(currentMember.isAdmin())
                .build();
    }
    
    private TeamInviteDto mapToDto(TeamInvite invite) {
        return TeamInviteDto.builder()
                .id(invite.getId())
                .inviteCode(invite.getInviteCode())
                .teamId(invite.getTeam().getId())
                .teamName(invite.getTeam().getName())
                .createdByDisplayName(invite.getCreatedBy().getDisplayName())
                .expiresAt(invite.getExpiresAt())
                .maxUses(invite.getMaxUses())
                .usedCount(invite.getUsedCount())
                .isActive(invite.getIsActive())
                .createdAt(invite.getCreatedAt())
                .isExpired(invite.isExpired())
                .isValid(invite.isValid())
                .remainingUses(invite.getRemainingUses())
                .build();
    }
}