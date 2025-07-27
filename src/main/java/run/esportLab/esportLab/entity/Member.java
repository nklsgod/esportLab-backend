package run.esportLab.esportLab.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Entity
@Table(name = "member")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Member {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "team_id", nullable = true)
    private Team team;
    
    @Column(name = "discord_user_id", unique = true, nullable = false)
    private String discordUserId;
    
    @Column(name = "display_name", nullable = false)
    private String displayName;
    
    @Column(name = "avatar_url", length = 500)
    private String avatarUrl;
    
    @Column(nullable = false)
    @Builder.Default
    private String tz = "Europe/Berlin";
    
    @Column
    private String roles;
    
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private ZonedDateTime createdAt;
    
    @OneToMany(mappedBy = "member", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<Availability> availabilities = new ArrayList<>();
    
    @OneToMany(mappedBy = "createdByMember", cascade = CascadeType.REMOVE)
    @Builder.Default
    private List<TrainingSession> createdTrainingSessions = new ArrayList<>();
    
    // Helper methods
    public void addAvailability(Availability availability) {
        availabilities.add(availability);
        availability.setMember(this);
    }
    
    public void removeAvailability(Availability availability) {
        availabilities.remove(availability);
        availability.setMember(null);
    }
    
    // Role management
    public List<String> getRolesList() {
        if (roles == null || roles.trim().isEmpty()) {
            return List.of();
        }
        return List.of(roles.split(","))
                .stream()
                .map(String::trim)
                .filter(role -> !role.isEmpty())
                .toList();
    }
    
    public void setRolesList(List<String> rolesList) {
        this.roles = rolesList != null ? String.join(",", rolesList) : null;
    }
    
    public boolean hasRole(String role) {
        return getRolesList().contains(role);
    }
    
    public boolean isAdmin() {
        return hasRole("ADMIN");
    }
    
    public boolean isPlayer() {
        return hasRole("PLAYER");
    }
    
    // Convenience method to get team IDs (for multi-team support later)
    public List<Long> getTeamIds() {
        return team != null ? List.of(team.getId()) : List.of();
    }
}