package run.esportLab.esportLab.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.ZonedDateTime;

@Entity
@Table(name = "job_lock")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class JobLock {
    
    @Id
    @Column(name = "key")
    private String key;
    
    @Column(name = "until", nullable = false)
    private ZonedDateTime until;
    
    // Helper methods
    public boolean isExpired() {
        return until.isBefore(ZonedDateTime.now());
    }
    
    public boolean isActive() {
        return !isExpired();
    }
    
    // Static factory methods for common lock types
    public static JobLock createReminderLock(String teamId, ZonedDateTime until) {
        return JobLock.builder()
                .key("reminder_job_team_" + teamId)
                .until(until)
                .build();
    }
    
    public static JobLock createWeeklySummaryLock(String teamId, ZonedDateTime until) {
        return JobLock.builder()
                .key("weekly_summary_team_" + teamId)
                .until(until)
                .build();
    }
    
    public static JobLock createTrainingPlannerLock(String teamId, ZonedDateTime until) {
        return JobLock.builder()
                .key("training_planner_team_" + teamId)
                .until(until)
                .build();
    }
    
    public static JobLock createGlobalLock(String jobType, ZonedDateTime until) {
        return JobLock.builder()
                .key("global_" + jobType)
                .until(until)
                .build();
    }
}