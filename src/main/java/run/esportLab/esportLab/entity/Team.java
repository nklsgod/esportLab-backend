package run.esportLab.esportLab.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "team")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Team {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false)
    private String name;
    
    @Column(name = "discord_guild_id", unique = true)
    private String discordGuildId;
    
    @Column(name = "reminder_channel_id")
    private String reminderChannelId;
    
    @Column(nullable = false)
    @Builder.Default
    private String tz = "Europe/Berlin";
    
    @Column(name = "min_players", nullable = false)
    @Builder.Default
    private Integer minPlayers = 4;
    
    @Column(name = "min_duration_minutes", nullable = false)
    @Builder.Default
    private Integer minDurationMinutes = 90;
    
    @Column(name = "reminder_hours", nullable = false)
    @Builder.Default
    private String reminderHours = "0,6,12,18";
    
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private ZonedDateTime createdAt;
    
    @OneToMany(mappedBy = "team", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<Member> members = new ArrayList<>();
    
    @OneToMany(mappedBy = "team", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<TrainingSession> trainingSessions = new ArrayList<>();
    
    // Helper methods
    public void addMember(Member member) {
        members.add(member);
        member.setTeam(this);
    }
    
    public void removeMember(Member member) {
        members.remove(member);
        member.setTeam(null);
    }
    
    public void addTrainingSession(TrainingSession session) {
        trainingSessions.add(session);
        session.setTeam(this);
    }
    
    public void removeTrainingSession(TrainingSession session) {
        trainingSessions.remove(session);
        session.setTeam(null);
    }
    
    // Parse reminder hours
    public List<Integer> getReminderHoursList() {
        if (reminderHours == null || reminderHours.trim().isEmpty()) {
            return List.of(0, 6, 12, 18);
        }
        return List.of(reminderHours.split(","))
                .stream()
                .map(String::trim)
                .map(Integer::parseInt)
                .toList();
    }
    
    public void setReminderHoursList(List<Integer> hours) {
        this.reminderHours = String.join(",", hours.stream().map(String::valueOf).toList());
    }
}