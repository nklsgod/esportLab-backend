package run.esportLab.esportLab.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import run.esportLab.esportLab.entity.Availability;

import java.time.ZonedDateTime;
import java.util.List;

@Repository
public interface AvailabilityRepository extends JpaRepository<Availability, Long> {
    
    /**
     * Find all availabilities for a specific member
     */
    List<Availability> findByMemberId(Long memberId);
    
    /**
     * Find availabilities for a member within a time range
     */
    @Query("SELECT a FROM Availability a WHERE a.member.id = :memberId " +
           "AND a.startsAtUtc < :endTime AND a.endsAtUtc > :startTime " +
           "ORDER BY a.startsAtUtc")
    List<Availability> findByMemberIdAndTimeRange(
            @Param("memberId") Long memberId,
            @Param("startTime") ZonedDateTime startTime,
            @Param("endTime") ZonedDateTime endTime
    );
    
    /**
     * Find all availabilities for team members within a time range
     */
    @Query("SELECT a FROM Availability a WHERE a.member.team.id = :teamId " +
           "AND a.startsAtUtc < :endTime AND a.endsAtUtc > :startTime " +
           "ORDER BY a.member.id, a.startsAtUtc")
    List<Availability> findByTeamIdAndTimeRange(
            @Param("teamId") Long teamId,
            @Param("startTime") ZonedDateTime startTime,
            @Param("endTime") ZonedDateTime endTime
    );
    
    /**
     * Find available time slots for team members within a time range
     */
    @Query("SELECT a FROM Availability a WHERE a.member.team.id = :teamId " +
           "AND a.available = true " +
           "AND a.startsAtUtc < :endTime AND a.endsAtUtc > :startTime " +
           "ORDER BY a.startsAtUtc")
    List<Availability> findAvailableByTeamIdAndTimeRange(
            @Param("teamId") Long teamId,
            @Param("startTime") ZonedDateTime startTime,
            @Param("endTime") ZonedDateTime endTime
    );
    
    /**
     * Find overlapping availabilities for a member (to prevent conflicts)
     */
    @Query("SELECT a FROM Availability a WHERE a.member.id = :memberId " +
           "AND a.id != :excludeId " +
           "AND a.startsAtUtc < :endTime AND a.endsAtUtc > :startTime")
    List<Availability> findOverlappingForMember(
            @Param("memberId") Long memberId,
            @Param("startTime") ZonedDateTime startTime,
            @Param("endTime") ZonedDateTime endTime,
            @Param("excludeId") Long excludeId
    );
    
    /**
     * Find overlapping availabilities for a new availability (excludeId can be null)
     */
    default List<Availability> findOverlappingForMember(
            Long memberId,
            ZonedDateTime startTime,
            ZonedDateTime endTime
    ) {
        return findOverlappingForMember(memberId, startTime, endTime, -1L);
    }
    
    /**
     * Find members without availability for a specific week
     */
    @Query("SELECT DISTINCT m.id FROM Member m " +
           "WHERE m.team.id = :teamId " +
           "AND m.id NOT IN (" +
           "    SELECT DISTINCT a.member.id FROM Availability a " +
           "    WHERE a.member.team.id = :teamId " +
           "    AND a.startsAtUtc < :weekEnd AND a.endsAtUtc > :weekStart" +
           ")")
    List<Long> findMemberIdsWithoutAvailabilityInWeek(
            @Param("teamId") Long teamId,
            @Param("weekStart") ZonedDateTime weekStart,
            @Param("weekEnd") ZonedDateTime weekEnd
    );
    
    /**
     * Delete old availabilities (cleanup)
     */
    @Query("DELETE FROM Availability a WHERE a.endsAtUtc < :cutoffDate")
    void deleteOldAvailabilities(@Param("cutoffDate") ZonedDateTime cutoffDate);
}