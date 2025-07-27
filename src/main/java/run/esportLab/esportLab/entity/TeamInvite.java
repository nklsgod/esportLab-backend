package run.esportLab.esportLab.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.ZonedDateTime;

@Entity
@Table(name = "team_invite")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TeamInvite {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "team_id", nullable = false)
    private Team team;
    
    @Column(name = "invite_code", unique = true, nullable = false)
    private String inviteCode;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by_member_id", nullable = false)
    private Member createdBy;
    
    @Column(name = "expires_at", nullable = false)
    private ZonedDateTime expiresAt;
    
    @Column(name = "max_uses")
    private Integer maxUses;
    
    @Column(name = "used_count", nullable = false)
    @Builder.Default
    private Integer usedCount = 0;
    
    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;
    
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private ZonedDateTime createdAt;
    
    // Helper methods
    public boolean isExpired() {
        return ZonedDateTime.now().isAfter(expiresAt);
    }
    
    public boolean isUsageLimitReached() {
        return maxUses != null && usedCount >= maxUses;
    }
    
    public boolean isValid() {
        return isActive && !isExpired() && !isUsageLimitReached();
    }
    
    public void incrementUsage() {
        this.usedCount++;
    }
    
    public void deactivate() {
        this.isActive = false;
    }
    
    public int getRemainingUses() {
        if (maxUses == null) {
            return Integer.MAX_VALUE; // Unlimited
        }
        return Math.max(0, maxUses - usedCount);
    }
}