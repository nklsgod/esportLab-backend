-- Make team_id nullable in member table to allow users to register without a team
ALTER TABLE member ALTER COLUMN team_id DROP NOT NULL;

-- Add team_invite table for managing team join codes
CREATE TABLE team_invite (
    id BIGSERIAL PRIMARY KEY,
    team_id BIGINT NOT NULL REFERENCES team(id) ON DELETE CASCADE,
    invite_code VARCHAR(16) UNIQUE NOT NULL,
    created_by_member_id BIGINT NOT NULL REFERENCES member(id) ON DELETE CASCADE,
    expires_at TIMESTAMPTZ NOT NULL,
    max_uses INTEGER NULL, -- NULL means unlimited
    used_count INTEGER NOT NULL DEFAULT 0,
    is_active BOOLEAN NOT NULL DEFAULT true,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- Indexes for performance
CREATE INDEX idx_team_invite_code ON team_invite(invite_code);
CREATE INDEX idx_team_invite_team_id ON team_invite(team_id);
CREATE INDEX idx_team_invite_expires_at ON team_invite(expires_at);
CREATE INDEX idx_team_invite_active ON team_invite(is_active) WHERE is_active = true;

-- Add constraint to prevent negative usage
ALTER TABLE team_invite ADD CONSTRAINT chk_team_invite_used_count_positive 
    CHECK (used_count >= 0);

-- Add constraint to prevent negative max_uses
ALTER TABLE team_invite ADD CONSTRAINT chk_team_invite_max_uses_positive 
    CHECK (max_uses IS NULL OR max_uses > 0);