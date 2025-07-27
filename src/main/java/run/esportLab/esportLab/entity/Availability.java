package run.esportLab.esportLab.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.ZonedDateTime;

@Entity
@Table(name = "availability")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Availability {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;
    
    @Column(name = "starts_at_utc", nullable = false)
    private ZonedDateTime startsAtUtc;
    
    @Column(name = "ends_at_utc", nullable = false)
    private ZonedDateTime endsAtUtc;
    
    @Column(nullable = false)
    @Builder.Default
    private Boolean available = true;
    
    @Column
    private String note;
    
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private ZonedDateTime createdAt;
    
    // Helper methods
    public boolean isAvailable() {
        return Boolean.TRUE.equals(available);
    }
    
    public boolean isUnavailable() {
        return Boolean.FALSE.equals(available);
    }
    
    // Check if this availability overlaps with another time range
    public boolean overlapsWith(ZonedDateTime otherStart, ZonedDateTime otherEnd) {
        return startsAtUtc.isBefore(otherEnd) && endsAtUtc.isAfter(otherStart);
    }
    
    // Check if this availability contains another time range
    public boolean contains(ZonedDateTime otherStart, ZonedDateTime otherEnd) {
        return !startsAtUtc.isAfter(otherStart) && !endsAtUtc.isBefore(otherEnd);
    }
    
    // Get duration in minutes
    public long getDurationMinutes() {
        return java.time.Duration.between(startsAtUtc, endsAtUtc).toMinutes();
    }
    
    // Validation helper
    public boolean isValidTimeRange() {
        return startsAtUtc != null && endsAtUtc != null && startsAtUtc.isBefore(endsAtUtc);
    }
    
    // Check if duration is within 24 hours
    public boolean isWithinMaxDuration() {
        return getDurationMinutes() <= 24 * 60; // 24 hours in minutes
    }
}