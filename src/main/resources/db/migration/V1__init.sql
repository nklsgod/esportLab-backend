-- V1__init.sql - Initial database schema for esports planner
-- UTC-based storage for all timestamps

-- Team table: represents an esports team with configuration
CREATE TABLE team (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    discord_guild_id VARCHAR(255) UNIQUE,
    reminder_channel_id VARCHAR(255),
    tz VARCHAR(50) NOT NULL DEFAULT 'Europe/Berlin',
    min_players INTEGER NOT NULL DEFAULT 4,
    min_duration_minutes INTEGER NOT NULL DEFAULT 90,
    reminder_hours TEXT NOT NULL DEFAULT '0,6,12,18', -- comma-separated hours
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- Member table: team members linked via Discord user ID
CREATE TABLE member (
    id BIGSERIAL PRIMARY KEY,
    team_id BIGINT NOT NULL,
    discord_user_id VARCHAR(255) UNIQUE NOT NULL,
    display_name VARCHAR(255) NOT NULL,
    avatar_url VARCHAR(500),
    tz VARCHAR(50) NOT NULL DEFAULT 'Europe/Berlin',
    roles TEXT, -- comma-separated roles like 'ADMIN,PLAYER'
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    
    CONSTRAINT fk_member_team FOREIGN KEY (team_id) REFERENCES team(id) ON DELETE CASCADE
);

-- Availability table: time slots when members are available/unavailable
CREATE TABLE availability (
    id BIGSERIAL PRIMARY KEY,
    member_id BIGINT NOT NULL,
    starts_at_utc TIMESTAMPTZ NOT NULL,
    ends_at_utc TIMESTAMPTZ NOT NULL,
    available BOOLEAN NOT NULL DEFAULT true,
    note TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    
    CONSTRAINT fk_availability_member FOREIGN KEY (member_id) REFERENCES member(id) ON DELETE CASCADE,
    CONSTRAINT chk_availability_time_order CHECK (starts_at_utc < ends_at_utc),
    CONSTRAINT chk_availability_max_duration CHECK (ends_at_utc - starts_at_utc <= INTERVAL '24 hours')
);

-- Training session table: scheduled training sessions (auto-generated or manual)
CREATE TABLE training_session (
    id BIGSERIAL PRIMARY KEY,
    team_id BIGINT NOT NULL,
    starts_at_utc TIMESTAMPTZ NOT NULL,
    ends_at_utc TIMESTAMPTZ NOT NULL,
    source VARCHAR(16) NOT NULL CHECK (source IN ('AUTO', 'MANUAL')),
    title VARCHAR(255),
    created_by_member_id BIGINT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    
    CONSTRAINT fk_training_session_team FOREIGN KEY (team_id) REFERENCES team(id) ON DELETE CASCADE,
    CONSTRAINT fk_training_session_creator FOREIGN KEY (created_by_member_id) REFERENCES member(id) ON DELETE SET NULL,
    CONSTRAINT chk_training_session_time_order CHECK (starts_at_utc < ends_at_utc)
);

-- Job lock table: distributed locking for scheduled jobs
CREATE TABLE job_lock (
    key VARCHAR(255) PRIMARY KEY,
    until TIMESTAMPTZ NOT NULL
);

-- Indexes for performance optimization
-- Availability queries by member and time range
CREATE INDEX idx_availability_member_time ON availability(member_id, starts_at_utc);
CREATE INDEX idx_availability_time_range ON availability(starts_at_utc, ends_at_utc);

-- Training session queries by team and time
CREATE INDEX idx_training_session_team_time ON training_session(team_id, starts_at_utc);
CREATE INDEX idx_training_session_time_range ON training_session(starts_at_utc, ends_at_utc);

-- Member lookup by Discord user ID
CREATE INDEX idx_member_discord_user_id ON member(discord_user_id);

-- Team lookup by Discord guild ID
CREATE INDEX idx_team_discord_guild_id ON team(discord_guild_id);

-- Job lock cleanup queries
CREATE INDEX idx_job_lock_until ON job_lock(until);

-- Additional constraints to prevent overlapping availability for same member
-- Note: This will be enforced at the application level to allow for more complex validation
-- CREATE UNIQUE INDEX idx_availability_no_overlap ON availability(member_id, tstzrange(starts_at_utc, ends_at_utc, '[)'))
-- WHERE available = true; -- PostgreSQL-specific, requires btree_gist extension