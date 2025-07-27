package run.esportLab.esportLab.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import run.esportLab.esportLab.entity.TeamInvite;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface TeamInviteRepository extends JpaRepository<TeamInvite, Long> {
    
    /**
     * Find a valid invite by code
     */
    @Query("SELECT ti FROM TeamInvite ti WHERE ti.inviteCode = :code " +
           "AND ti.isActive = true " +
           "AND ti.expiresAt > :now " +
           "AND (ti.maxUses IS NULL OR ti.usedCount < ti.maxUses)")
    Optional<TeamInvite> findValidInviteByCode(@Param("code") String code, @Param("now") ZonedDateTime now);
    
    /**
     * Find all active invites for a team
     */
    @Query("SELECT ti FROM TeamInvite ti WHERE ti.team.id = :teamId " +
           "AND ti.isActive = true " +
           "ORDER BY ti.createdAt DESC")
    List<TeamInvite> findActiveInvitesByTeamId(@Param("teamId") Long teamId);
    
    /**
     * Find all invites created by a member
     */
    List<TeamInvite> findByCreatedByIdOrderByCreatedAtDesc(Long memberId);
    
    /**
     * Check if invite code already exists
     */
    boolean existsByInviteCode(String inviteCode);
    
    /**
     * Deactivate all invites for a team (when team is deleted)
     */
    @Modifying
    @Query("UPDATE TeamInvite ti SET ti.isActive = false WHERE ti.team.id = :teamId")
    void deactivateAllInvitesForTeam(@Param("teamId") Long teamId);
    
    /**
     * Clean up expired invites
     */
    @Modifying
    @Query("UPDATE TeamInvite ti SET ti.isActive = false WHERE ti.expiresAt < :now AND ti.isActive = true")
    int deactivateExpiredInvites(@Param("now") ZonedDateTime now);
    
    /**
     * Delete old inactive invites (cleanup job)
     */
    @Modifying
    @Query("DELETE FROM TeamInvite ti WHERE ti.isActive = false AND ti.createdAt < :cutoffDate")
    void deleteOldInactiveInvites(@Param("cutoffDate") ZonedDateTime cutoffDate);
    
    /**
     * Count active invites for a team
     */
    @Query("SELECT COUNT(ti) FROM TeamInvite ti WHERE ti.team.id = :teamId AND ti.isActive = true")
    long countActiveInvitesByTeamId(@Param("teamId") Long teamId);
}