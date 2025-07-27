package run.esportLab.esportLab.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import run.esportLab.esportLab.entity.Team;

import java.util.Optional;

@Repository
public interface TeamRepository extends JpaRepository<Team, Long> {
    
    /**
     * Find team by Discord guild ID
     */
    Optional<Team> findByDiscordGuildId(String discordGuildId);
    
    /**
     * Check if a team exists with the given Discord guild ID
     */
    boolean existsByDiscordGuildId(String discordGuildId);
    
    /**
     * Find team with members loaded
     */
    @Query("SELECT t FROM Team t LEFT JOIN FETCH t.members WHERE t.id = :id")
    Optional<Team> findByIdWithMembers(@Param("id") Long id);
    
    /**
     * Find team with training sessions loaded
     */
    @Query("SELECT t FROM Team t LEFT JOIN FETCH t.trainingSessions WHERE t.id = :id")
    Optional<Team> findByIdWithTrainingSessions(@Param("id") Long id);
    
    /**
     * Find team with all relationships loaded
     */
    @Query("SELECT DISTINCT t FROM Team t " +
           "LEFT JOIN FETCH t.members m " +
           "LEFT JOIN FETCH t.trainingSessions " +
           "WHERE t.id = :id")
    Optional<Team> findByIdWithAllRelations(@Param("id") Long id);
}