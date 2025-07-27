package run.esportLab.esportLab.repository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import run.esportLab.esportLab.entity.*;

import java.time.ZonedDateTime;
import java.time.ZoneId;
import java.util.List;

import static org.assertj.core.api.Assertions.*;

@SpringBootTest
@Testcontainers
@Transactional
class DataModelIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("esports_test")
            .withUsername("test")
            .withPassword("test");

    @org.springframework.boot.test.context.TestConfiguration
    static class TestConfig {
        @org.springframework.context.annotation.Bean
        @org.springframework.context.annotation.Primary
        public javax.sql.DataSource dataSource() {
            org.springframework.boot.jdbc.DataSourceBuilder<?> builder = org.springframework.boot.jdbc.DataSourceBuilder.create()
                    .url(postgres.getJdbcUrl())
                    .username(postgres.getUsername())
                    .password(postgres.getPassword())
                    .driverClassName("org.postgresql.Driver");
            return builder.build();
        }
    }

    @Autowired
    private TeamRepository teamRepository;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private AvailabilityRepository availabilityRepository;

    @Autowired
    private TrainingSessionRepository trainingSessionRepository;

    @Autowired
    private JobLockRepository jobLockRepository;

    private Team testTeam;
    private Member testMember;

    @BeforeEach
    void setUp() {
        // Set up test data
        testTeam = Team.builder()
                .name("Test Team")
                .discordGuildId("123456789")
                .reminderChannelId("987654321")
                .tz("Europe/Berlin")
                .minPlayers(4)
                .minDurationMinutes(90)
                .reminderHours("0,6,12,18")
                .build();

        testTeam = teamRepository.save(testTeam);

        testMember = Member.builder()
                .team(testTeam)
                .discordUserId("555666777")
                .displayName("Test Player")
                .avatarUrl("https://example.com/avatar.jpg")
                .tz("Europe/Berlin")
                .roles("PLAYER,ADMIN")
                .build();

        testMember = memberRepository.save(testMember);
    }

    @Test
    void testTeamCRUD() {
        // Test Create
        Team team = Team.builder()
                .name("Another Team")
                .discordGuildId("111222333")
                .tz("UTC")
                .build();

        Team savedTeam = teamRepository.save(team);
        assertThat(savedTeam.getId()).isNotNull();
        assertThat(savedTeam.getName()).isEqualTo("Another Team");
        assertThat(savedTeam.getDiscordGuildId()).isEqualTo("111222333");
        assertThat(savedTeam.getCreatedAt()).isNotNull();

        // Test Read
        Team foundTeam = teamRepository.findById(savedTeam.getId()).orElse(null);
        assertThat(foundTeam).isNotNull();
        assertThat(foundTeam.getName()).isEqualTo("Another Team");

        // Test findByDiscordGuildId
        Team foundByGuildId = teamRepository.findByDiscordGuildId("111222333").orElse(null);
        assertThat(foundByGuildId).isNotNull();
        assertThat(foundByGuildId.getId()).isEqualTo(savedTeam.getId());

        // Test Update
        foundTeam.setName("Updated Team Name");
        Team updatedTeam = teamRepository.save(foundTeam);
        assertThat(updatedTeam.getName()).isEqualTo("Updated Team Name");

        // Test Delete
        teamRepository.delete(updatedTeam);
        assertThat(teamRepository.findById(savedTeam.getId())).isEmpty();
    }

    @Test
    void testMemberCRUD() {
        // Test Create
        Member member = Member.builder()
                .team(testTeam)
                .discordUserId("999888777")
                .displayName("New Player")
                .tz("America/New_York")
                .roles("PLAYER")
                .build();

        Member savedMember = memberRepository.save(member);
        assertThat(savedMember.getId()).isNotNull();
        assertThat(savedMember.getDiscordUserId()).isEqualTo("999888777");
        assertThat(savedMember.getCreatedAt()).isNotNull();

        // Test Read
        Member foundMember = memberRepository.findByDiscordUserId("999888777").orElse(null);
        assertThat(foundMember).isNotNull();
        assertThat(foundMember.getDisplayName()).isEqualTo("New Player");

        // Test role management
        assertThat(foundMember.getRolesList()).containsExactly("PLAYER");
        assertThat(foundMember.isPlayer()).isTrue();
        assertThat(foundMember.isAdmin()).isFalse();

        // Test team relationship
        assertThat(foundMember.getTeam().getId()).isEqualTo(testTeam.getId());

        // Test findByTeamId
        List<Member> teamMembers = memberRepository.findByTeamId(testTeam.getId());
        assertThat(teamMembers).hasSize(2); // testMember + savedMember

        // Test Update
        foundMember.setDisplayName("Updated Player");
        foundMember.setRolesList(List.of("PLAYER", "ADMIN"));
        Member updatedMember = memberRepository.save(foundMember);
        assertThat(updatedMember.getDisplayName()).isEqualTo("Updated Player");
        assertThat(updatedMember.isAdmin()).isTrue();

        // Test Delete
        memberRepository.delete(updatedMember);
        assertThat(memberRepository.findByDiscordUserId("999888777")).isEmpty();
    }

    @Test
    void testAvailabilityCRUD() {
        ZonedDateTime start = ZonedDateTime.now(ZoneId.of("UTC")).plusDays(1);
        ZonedDateTime end = start.plusHours(3);

        // Test Create
        Availability availability = Availability.builder()
                .member(testMember)
                .startsAtUtc(start)
                .endsAtUtc(end)
                .available(true)
                .note("Available for training")
                .build();

        Availability savedAvailability = availabilityRepository.save(availability);
        assertThat(savedAvailability.getId()).isNotNull();
        assertThat(savedAvailability.getStartsAtUtc()).isEqualTo(start);
        assertThat(savedAvailability.getEndsAtUtc()).isEqualTo(end);
        assertThat(savedAvailability.isAvailable()).isTrue();
        assertThat(savedAvailability.getCreatedAt()).isNotNull();

        // Test Read
        List<Availability> memberAvailabilities = availabilityRepository.findByMemberId(testMember.getId());
        assertThat(memberAvailabilities).hasSize(1);
        assertThat(memberAvailabilities.get(0).getNote()).isEqualTo("Available for training");

        // Test time range queries
        ZonedDateTime searchStart = start.minusHours(1);
        ZonedDateTime searchEnd = end.plusHours(1);
        List<Availability> timeRangeAvailabilities = availabilityRepository
                .findByMemberIdAndTimeRange(testMember.getId(), searchStart, searchEnd);
        assertThat(timeRangeAvailabilities).hasSize(1);

        // Test team availability queries
        List<Availability> teamAvailabilities = availabilityRepository
                .findByTeamIdAndTimeRange(testTeam.getId(), searchStart, searchEnd);
        assertThat(teamAvailabilities).hasSize(1);

        // Test helper methods
        assertThat(savedAvailability.getDurationMinutes()).isEqualTo(180); // 3 hours
        assertThat(savedAvailability.isValidTimeRange()).isTrue();
        assertThat(savedAvailability.isWithinMaxDuration()).isTrue();

        // Test Update
        savedAvailability.setNote("Updated note");
        savedAvailability.setAvailable(false);
        Availability updatedAvailability = availabilityRepository.save(savedAvailability);
        assertThat(updatedAvailability.getNote()).isEqualTo("Updated note");
        assertThat(updatedAvailability.isUnavailable()).isTrue();

        // Test Delete
        availabilityRepository.delete(updatedAvailability);
        assertThat(availabilityRepository.findByMemberId(testMember.getId())).isEmpty();
    }

    @Test
    void testTrainingSessionCRUD() {
        ZonedDateTime start = ZonedDateTime.now(ZoneId.of("UTC")).plusDays(2);
        ZonedDateTime end = start.plusHours(2);

        // Test Create
        TrainingSession session = TrainingSession.builder()
                .team(testTeam)
                .startsAtUtc(start)
                .endsAtUtc(end)
                .source(TrainingSession.Source.AUTO)
                .title("Auto-generated Training")
                .createdByMember(testMember)
                .build();

        TrainingSession savedSession = trainingSessionRepository.save(session);
        assertThat(savedSession.getId()).isNotNull();
        assertThat(savedSession.getStartsAtUtc()).isEqualTo(start);
        assertThat(savedSession.getSource()).isEqualTo(TrainingSession.Source.AUTO);
        assertThat(savedSession.getCreatedAt()).isNotNull();

        // Test Read
        List<TrainingSession> teamSessions = trainingSessionRepository.findByTeamId(testTeam.getId());
        assertThat(teamSessions).hasSize(1);

        // Test time range queries
        ZonedDateTime searchStart = start.minusHours(1);
        ZonedDateTime searchEnd = end.plusHours(1);
        List<TrainingSession> timeRangeSessions = trainingSessionRepository
                .findByTeamIdAndTimeRange(testTeam.getId(), searchStart, searchEnd);
        assertThat(timeRangeSessions).hasSize(1);

        // Test source-specific queries
        List<TrainingSession> autoSessions = trainingSessionRepository
                .findAutoGeneratedByTeamIdAndTimeRange(testTeam.getId(), searchStart, searchEnd);
        assertThat(autoSessions).hasSize(1);

        List<TrainingSession> manualSessions = trainingSessionRepository
                .findManualByTeamIdAndTimeRange(testTeam.getId(), searchStart, searchEnd);
        assertThat(manualSessions).isEmpty();

        // Test helper methods
        assertThat(savedSession.isAutoGenerated()).isTrue();
        assertThat(savedSession.isManuallyCreated()).isFalse();
        assertThat(savedSession.getDurationMinutes()).isEqualTo(120); // 2 hours
        assertThat(savedSession.getEffectiveTitle()).isEqualTo("Auto-generated Training");

        // Test Update
        savedSession.setTitle("Updated Training");
        savedSession.setSource(TrainingSession.Source.MANUAL);
        TrainingSession updatedSession = trainingSessionRepository.save(savedSession);
        assertThat(updatedSession.getTitle()).isEqualTo("Updated Training");
        assertThat(updatedSession.isManuallyCreated()).isTrue();

        // Test Delete
        trainingSessionRepository.delete(updatedSession);
        assertThat(trainingSessionRepository.findByTeamId(testTeam.getId())).isEmpty();
    }

    @Test
    void testJobLockCRUD() {
        ZonedDateTime until = ZonedDateTime.now(ZoneId.of("UTC")).plusMinutes(30);

        // Test Create
        JobLock lock = JobLock.builder()
                .key("test_job_lock")
                .until(until)
                .build();

        JobLock savedLock = jobLockRepository.save(lock);
        assertThat(savedLock.getKey()).isEqualTo("test_job_lock");
        assertThat(savedLock.getUntil()).isEqualTo(until);

        // Test Read
        JobLock foundLock = jobLockRepository.findById("test_job_lock").orElse(null);
        assertThat(foundLock).isNotNull();
        assertThat(foundLock.isActive()).isTrue();
        assertThat(foundLock.isExpired()).isFalse();

        // Test active lock queries
        List<JobLock> activeLocks = jobLockRepository.findActiveLocks(ZonedDateTime.now(ZoneId.of("UTC")));
        assertThat(activeLocks).hasSize(1);

        boolean canAcquire = jobLockRepository.canAcquireLock("test_job_lock", ZonedDateTime.now(ZoneId.of("UTC")));
        assertThat(canAcquire).isFalse(); // Lock is active

        // Test factory methods
        JobLock reminderLock = JobLock.createReminderLock("123", until);
        assertThat(reminderLock.getKey()).isEqualTo("reminder_job_team_123");

        JobLock summaryLock = JobLock.createWeeklySummaryLock("123", until);
        assertThat(summaryLock.getKey()).isEqualTo("weekly_summary_team_123");

        // Test Update
        ZonedDateTime newUntil = until.plusHours(1);
        foundLock.setUntil(newUntil);
        JobLock updatedLock = jobLockRepository.save(foundLock);
        assertThat(updatedLock.getUntil()).isEqualTo(newUntil);

        // Test Delete
        jobLockRepository.delete(updatedLock);
        assertThat(jobLockRepository.findById("test_job_lock")).isEmpty();
    }

    @Test
    void testDatabaseConstraints() {
        // Test time order constraint in availability
        ZonedDateTime start = ZonedDateTime.now(ZoneId.of("UTC"));
        ZonedDateTime invalidEnd = start.minusHours(1); // End before start

        Availability invalidAvailability = Availability.builder()
                .member(testMember)
                .startsAtUtc(start)
                .endsAtUtc(invalidEnd)
                .available(true)
                .build();

        // This should fail due to database constraint
        assertThatThrownBy(() -> {
            availabilityRepository.save(invalidAvailability);
            availabilityRepository.flush(); // Force database interaction
        }).isInstanceOf(Exception.class);
    }

    @Test
    void testForeignKeyRelationships() {
        // Test that foreign key relationships work correctly
        
        // Verify member belongs to team
        Member foundMember = memberRepository.findById(testMember.getId()).orElseThrow();
        assertThat(foundMember.getTeam().getId()).isEqualTo(testTeam.getId());
        
        // Create availability and verify relationship
        Availability availability = Availability.builder()
                .member(testMember)
                .startsAtUtc(ZonedDateTime.now(ZoneId.of("UTC")).plusDays(1))
                .endsAtUtc(ZonedDateTime.now(ZoneId.of("UTC")).plusDays(1).plusHours(2))
                .available(true)
                .build();
        availability = availabilityRepository.save(availability);
        
        assertThat(availability.getMember().getId()).isEqualTo(testMember.getId());
        
        // Create training session and verify relationship
        TrainingSession session = TrainingSession.builder()
                .team(testTeam)
                .startsAtUtc(ZonedDateTime.now(ZoneId.of("UTC")).plusDays(2))
                .endsAtUtc(ZonedDateTime.now(ZoneId.of("UTC")).plusDays(2).plusHours(2))
                .source(TrainingSession.Source.MANUAL)
                .createdByMember(testMember)
                .build();
        session = trainingSessionRepository.save(session);
        
        assertThat(session.getTeam().getId()).isEqualTo(testTeam.getId());
        assertThat(session.getCreatedByMember().getId()).isEqualTo(testMember.getId());
        
        // Clean up
        availabilityRepository.delete(availability);
        trainingSessionRepository.delete(session);
    }
}