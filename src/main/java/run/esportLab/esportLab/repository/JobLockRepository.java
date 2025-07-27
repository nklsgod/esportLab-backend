package run.esportLab.esportLab.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import run.esportLab.esportLab.entity.JobLock;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface JobLockRepository extends JpaRepository<JobLock, String> {
    
    /**
     * Find active locks (not expired)
     */
    @Query("SELECT jl FROM JobLock jl WHERE jl.until > :now")
    List<JobLock> findActiveLocks(@Param("now") ZonedDateTime now);
    
    /**
     * Find expired locks
     */
    @Query("SELECT jl FROM JobLock jl WHERE jl.until <= :now")
    List<JobLock> findExpiredLocks(@Param("now") ZonedDateTime now);
    
    /**
     * Check if a lock is active (not expired)
     */
    @Query("SELECT jl FROM JobLock jl WHERE jl.key = :key AND jl.until > :now")
    Optional<JobLock> findActiveLock(@Param("key") String key, @Param("now") ZonedDateTime now);
    
    /**
     * Try to acquire a lock (insert if not exists and not active)
     * Returns true if lock was acquired, false if already exists and active
     */
    @Query("SELECT CASE WHEN COUNT(jl) = 0 THEN true ELSE false END " +
           "FROM JobLock jl WHERE jl.key = :key AND jl.until > :now")
    boolean canAcquireLock(@Param("key") String key, @Param("now") ZonedDateTime now);
    
    /**
     * Delete expired locks (cleanup)
     */
    @Modifying
    @Query("DELETE FROM JobLock jl WHERE jl.until <= :now")
    int deleteExpiredLocks(@Param("now") ZonedDateTime now);
    
    /**
     * Delete lock by key (release lock)
     */
    @Modifying
    @Query("DELETE FROM JobLock jl WHERE jl.key = :key")
    int deleteLockByKey(@Param("key") String key);
    
    /**
     * Update lock expiration time (extend lock)
     */
    @Modifying
    @Query("UPDATE JobLock jl SET jl.until = :newUntil WHERE jl.key = :key")
    int extendLock(@Param("key") String key, @Param("newUntil") ZonedDateTime newUntil);
    
    /**
     * Force acquire lock (upsert - insert or update)
     * This will either create a new lock or update existing one
     */
    @Modifying
    @Query(value = "INSERT INTO job_lock (key, until) VALUES (:key, :until) " +
                   "ON CONFLICT (key) DO UPDATE SET until = :until",
           nativeQuery = true)
    void forceAcquireLock(@Param("key") String key, @Param("until") ZonedDateTime until);
}