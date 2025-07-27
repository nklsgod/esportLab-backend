package run.esportLab.esportLab.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import run.esportLab.esportLab.entity.Member;

import java.util.List;
import java.util.Optional;

@Repository
public interface MemberRepository extends JpaRepository<Member, Long> {
    
    /**
     * Find member by Discord user ID
     */
    Optional<Member> findByDiscordUserId(String discordUserId);
    
    /**
     * Check if a member exists with the given Discord user ID
     */
    boolean existsByDiscordUserId(String discordUserId);
    
    /**
     * Find all members of a specific team
     */
    List<Member> findByTeamId(Long teamId);
    
    /**
     * Find member by Discord user ID with team loaded
     */
    @Query("SELECT m FROM Member m LEFT JOIN FETCH m.team WHERE m.discordUserId = :discordUserId")
    Optional<Member> findByDiscordUserIdWithTeam(@Param("discordUserId") String discordUserId);
    
    /**
     * Find member with availabilities loaded
     */
    @Query("SELECT m FROM Member m LEFT JOIN FETCH m.availabilities WHERE m.id = :id")
    Optional<Member> findByIdWithAvailabilities(@Param("id") Long id);
    
    /**
     * Find all members of a team with their availabilities
     */
    @Query("SELECT DISTINCT m FROM Member m " +
           "LEFT JOIN FETCH m.availabilities " +
           "WHERE m.team.id = :teamId")
    List<Member> findByTeamIdWithAvailabilities(@Param("teamId") Long teamId);
    
    /**
     * Find members by role containing a specific role
     */
    @Query("SELECT m FROM Member m WHERE m.roles LIKE %:role%")
    List<Member> findByRolesContaining(@Param("role") String role);
    
    /**
     * Find team admins
     */
    @Query("SELECT m FROM Member m WHERE m.roles LIKE '%ADMIN%'")
    List<Member> findAdmins();
    
    /**
     * Find team admins for a specific team
     */
    @Query("SELECT m FROM Member m WHERE m.team.id = :teamId AND m.roles LIKE '%ADMIN%'")
    List<Member> findAdminsByTeamId(@Param("teamId") Long teamId);
    
    /**
     * Find members by team ID and roles containing a specific role
     */
    @Query("SELECT m FROM Member m WHERE m.team.id = :teamId AND m.roles LIKE %:role%")
    List<Member> findByTeamIdAndRolesContaining(@Param("teamId") Long teamId, @Param("role") String role);
}